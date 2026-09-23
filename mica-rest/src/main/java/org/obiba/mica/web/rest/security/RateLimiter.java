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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * In memory, per client IP, rate limiting of the anonymous endpoints that are prone to abuse (contact form, sign up,
 * password reset). The limits are per node: each Mica instance counts its own requests.
 * <p>
 * The client IP is the remote address of the request, unless this remote address is a trusted proxy: the
 * <code>X-Forwarded-For</code> (or <code>Forwarded</code>) header is then read from right to left, as long as the
 * hop that appended the address is itself a trusted proxy. Forwarding headers sent by any other peer are ignored, so
 * that they cannot be spoofed to get around the limit.
 */
@Component
public class RateLimiter {

  private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

  private static final String PREFIX = "security.rateLimit.";

  private static final String DEFAULT_TRUSTED_PROXIES = "127.0.0.1,::1";

  private static final long MAX_TRACKED_CLIENTS = 100000;

  public enum Bucket {
    CONTACT("contact", 5, 600),
    SIGNUP("signup", 10, 3600),
    FORGOT_PASSWORD("forgotPassword", 5, 600);

    private final String name;

    private final int defaultMaxRequests;

    private final int defaultPeriodSeconds;

    Bucket(String name, int defaultMaxRequests, int defaultPeriodSeconds) {
      this.name = name;
      this.defaultMaxRequests = defaultMaxRequests;
      this.defaultPeriodSeconds = defaultPeriodSeconds;
    }
  }

  record Limit(int maxRequests, int periodSeconds) {}

  private record Window(long start, int count) {}

  private final boolean enabled;

  private final List<IpRange> trustedProxies;

  private final Map<Bucket, Limit> limits = new EnumMap<>(Bucket.class);

  private final Map<Bucket, Cache<String, Window>> windows = new EnumMap<>(Bucket.class);

  private final Ticker ticker;

  @Inject
  public RateLimiter(Environment environment) {
    this(environment.getProperty(PREFIX + "enabled", Boolean.class, true),
      environment.getProperty("security.trustedProxies", DEFAULT_TRUSTED_PROXIES),
      readLimits(environment),
      Ticker.systemTicker());
  }

  RateLimiter(boolean enabled, String trustedProxies, Map<Bucket, Limit> limits, Ticker ticker) {
    this.enabled = enabled;
    this.trustedProxies = parseTrustedProxies(trustedProxies);
    this.ticker = ticker;
    for (Bucket bucket : Bucket.values()) {
      Limit limit = limits.getOrDefault(bucket, new Limit(bucket.defaultMaxRequests, bucket.defaultPeriodSeconds));
      this.limits.put(bucket, limit);
      windows.put(bucket, CacheBuilder.newBuilder()
        .ticker(ticker)
        .expireAfterWrite(Math.max(1, limit.periodSeconds()), TimeUnit.SECONDS)
        .maximumSize(MAX_TRACKED_CLIENTS)
        .build());
    }
  }

  /**
   * Count the request against the client's quota for the bucket.
   *
   * @throws RateLimitExceededException when the quota is exhausted
   */
  public void check(Bucket bucket, HttpServletRequest request) {
    if (!enabled) return;
    String ip = getClientIP(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), request.getHeader("Forwarded"));
    long retryAfter = acquire(bucket, ip);
    if (retryAfter > 0) {
      log.warn("Rate limit exceeded on {} by {}, retry after {}s", bucket.name, ip, retryAfter);
      throw new RateLimitExceededException(retryAfter);
    }
  }

  /**
   * @return 0 when the request is allowed, otherwise the number of seconds before the client can try again
   */
  long acquire(Bucket bucket, String ip) {
    Limit limit = limits.get(bucket);
    if (limit.maxRequests() <= 0 || limit.periodSeconds() <= 0) return 0;
    long now = ticker.read();
    long period = TimeUnit.SECONDS.toNanos(limit.periodSeconds());
    Window window = windows.get(bucket).asMap().compute(ip, (key, current) ->
      current == null || now - current.start() >= period ? new Window(now, 1) : new Window(current.start(), current.count() + 1));
    if (window.count() <= limit.maxRequests()) return 0;
    return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(period - (now - window.start()) + TimeUnit.SECONDS.toNanos(1) - 1));
  }

  String getClientIP(String remoteAddr, String xForwardedFor, String forwarded) {
    String ip = normalize(remoteAddr);
    if (ip == null) return Strings.nullToEmpty(remoteAddr);
    List<String> chain = !Strings.isNullOrEmpty(xForwardedFor) ? parseXForwardedFor(xForwardedFor) : parseForwarded(forwarded);
    // walk the chain from the closest hop, trusting an address only when it was appended by a trusted proxy
    for (String hop : Lists.reverse(chain)) {
      if (!isTrustedProxy(ip)) break;
      String hopIp = normalize(hop);
      if (hopIp == null) break;
      ip = hopIp;
    }
    return ip;
  }

  private boolean isTrustedProxy(String ip) {
    InetAddress address = InetAddresses.forString(ip);
    return trustedProxies.stream().anyMatch(range -> range.contains(address));
  }

  private static List<String> parseXForwardedFor(String header) {
    return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(header);
  }

  /**
   * Extract the <code>for</code> parameters of a RFC 7239 <code>Forwarded</code> header.
   */
  private static List<String> parseForwarded(String header) {
    List<String> addresses = Lists.newArrayList();
    if (Strings.isNullOrEmpty(header)) return addresses;
    for (String element : Splitter.on(',').trimResults().omitEmptyStrings().split(header)) {
      String forValue = "";
      for (String pair : Splitter.on(';').trimResults().split(element)) {
        int idx = pair.indexOf('=');
        if (idx > 0 && "for".equalsIgnoreCase(pair.substring(0, idx).trim())) {
          forValue = pair.substring(idx + 1).trim();
        }
      }
      // an element without a usable "for" breaks the chain of trust, keep it as an invalid hop
      addresses.add(forValue);
    }
    return addresses;
  }

  /**
   * Normalize an address, removing quotes, IPv6 brackets and port.
   *
   * @return null if it is not an IP address
   */
  static String normalize(String value) {
    if (Strings.isNullOrEmpty(value)) return null;
    String ip = value.trim();
    if (ip.length() > 1 && ip.startsWith("\"") && ip.endsWith("\"")) ip = ip.substring(1, ip.length() - 1);
    if (ip.startsWith("[")) {
      int end = ip.indexOf(']');
      if (end < 0) return null;
      ip = ip.substring(1, end);
    } else if (ip.indexOf(':') > 0 && ip.indexOf(':') == ip.lastIndexOf(':')) {
      // IPv4 with port
      ip = ip.substring(0, ip.indexOf(':'));
    }
    if (!InetAddresses.isInetAddress(ip)) return null;
    return InetAddresses.toAddrString(InetAddresses.forString(ip));
  }

  private static Map<Bucket, Limit> readLimits(Environment environment) {
    Map<Bucket, Limit> limits = new EnumMap<>(Bucket.class);
    for (Bucket bucket : Bucket.values()) {
      limits.put(bucket, new Limit(
        environment.getProperty(PREFIX + bucket.name + ".maxRequests", Integer.class, bucket.defaultMaxRequests),
        environment.getProperty(PREFIX + bucket.name + ".periodSeconds", Integer.class, bucket.defaultPeriodSeconds)));
    }
    return limits;
  }

  private static List<IpRange> parseTrustedProxies(String value) {
    List<IpRange> ranges = Lists.newArrayList();
    for (String item : Splitter.on(',').trimResults().omitEmptyStrings().split(Strings.nullToEmpty(value))) {
      try {
        ranges.add(IpRange.parse(item));
      } catch (IllegalArgumentException e) {
        log.warn("Ignoring invalid trusted proxy address: {}", item);
      }
    }
    return ranges;
  }

  /**
   * An IP address or a CIDR block.
   */
  private record IpRange(byte[] network, int prefixLength) {

    static IpRange parse(String value) {
      int idx = value.indexOf('/');
      byte[] network = InetAddresses.forString(idx < 0 ? value : value.substring(0, idx)).getAddress();
      int prefixLength = idx < 0 ? network.length * 8 : Integer.parseInt(value.substring(idx + 1));
      if (prefixLength < 0 || prefixLength > network.length * 8)
        throw new IllegalArgumentException("Invalid prefix length: " + value);
      return new IpRange(network, prefixLength);
    }

    boolean contains(InetAddress address) {
      byte[] bytes = address.getAddress();
      if (bytes.length != network.length) return false;
      int fullBytes = prefixLength / 8;
      for (int i = 0; i < fullBytes; i++) {
        if (bytes[i] != network[i]) return false;
      }
      int remainingBits = prefixLength % 8;
      if (remainingBits == 0) return true;
      int mask = (0xFF << (8 - remainingBits)) & 0xFF;
      return (bytes[fullBytes] & mask) == (network[fullBytes] & mask);
    }
  }
}
