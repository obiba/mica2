/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RateLimiterTest {

  private final AtomicLong nanos = new AtomicLong();

  private final Ticker ticker = new Ticker() {
    @Override
    public long read() {
      return nanos.get();
    }
  };

  private RateLimiter newRateLimiter(String trustedProxies) {
    return new RateLimiter(true, trustedProxies,
      ImmutableMap.of(RateLimiter.Bucket.CONTACT, new RateLimiter.Limit(3, 600)), ticker);
  }

  @Test
  public void test_limit_per_ip() {
    RateLimiter rateLimiter = newRateLimiter("");
    for (int i = 0; i < 3; i++) {
      assertThat(rateLimiter.acquire(RateLimiter.Bucket.CONTACT, "10.0.0.1"), is(0L));
    }
    assertThat(rateLimiter.acquire(RateLimiter.Bucket.CONTACT, "10.0.0.1"), is(600L));
    // other clients and other buckets are counted separately
    assertThat(rateLimiter.acquire(RateLimiter.Bucket.CONTACT, "10.0.0.2"), is(0L));
    assertThat(rateLimiter.acquire(RateLimiter.Bucket.FORGOT_PASSWORD, "10.0.0.1"), is(0L));

    nanos.addAndGet(TimeUnit.SECONDS.toNanos(599) + 1);
    assertThat(rateLimiter.acquire(RateLimiter.Bucket.CONTACT, "10.0.0.1"), is(1L));

    nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));
    assertThat(rateLimiter.acquire(RateLimiter.Bucket.CONTACT, "10.0.0.1"), is(0L));
  }

  @Test
  public void test_forwarded_headers_ignored_from_untrusted_peer() {
    RateLimiter rateLimiter = newRateLimiter("127.0.0.1,::1");
    assertThat(rateLimiter.getClientIP("203.0.113.9", "1.2.3.4", null), is("203.0.113.9"));
    assertThat(rateLimiter.getClientIP("203.0.113.9", null, "for=1.2.3.4"), is("203.0.113.9"));
  }

  @Test
  public void test_forwarded_headers_from_trusted_proxy() {
    RateLimiter rateLimiter = newRateLimiter("127.0.0.1,::1,10.0.0.0/8");
    assertThat(rateLimiter.getClientIP("127.0.0.1", "198.51.100.7", null), is("198.51.100.7"));
    assertThat(rateLimiter.getClientIP("0:0:0:0:0:0:0:1", "198.51.100.7", null), is("198.51.100.7"));
    // spoofed left-most entries are not trusted
    assertThat(rateLimiter.getClientIP("127.0.0.1", "1.2.3.4, 198.51.100.7, 10.1.2.3", null), is("198.51.100.7"));
    // all hops trusted
    assertThat(rateLimiter.getClientIP("127.0.0.1", "10.1.2.3", null), is("10.1.2.3"));
    // invalid entry stops the walk
    assertThat(rateLimiter.getClientIP("127.0.0.1", "unknown", null), is("127.0.0.1"));
    assertThat(rateLimiter.getClientIP("127.0.0.1", "198.51.100.7:4711", null), is("198.51.100.7"));
    assertThat(rateLimiter.getClientIP("127.0.0.1", null,
      "for=1.2.3.4, for=\"[2001:db8::1]:4711\";proto=https"), is("2001:db8::1"));
  }
}
