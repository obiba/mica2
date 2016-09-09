/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.runtime.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import static com.google.common.base.Strings.isNullOrEmpty;

@Configuration
@EnableMongoRepositories("org.obiba.mica")
public class MongoDbConfiguration extends AbstractMongoConfiguration implements EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(MongoDbConfiguration.class);

  private RelaxedPropertyResolver propertyResolver;

  private MongoClientURI clientUri;

  @Inject
  private Environment env;

  @Override
  public void setEnvironment(Environment environment) {
    propertyResolver = new RelaxedPropertyResolver(environment, "mongodb.");
  }

  @Override
  protected String getDatabaseName() {
    return propertyResolver.getProperty("databaseName");
  }

  @Override
  public Mongo mongo() throws Exception {
    log.debug("Configuring MongoDB");
    clientUri = buildUri();
    return new MongoClient(clientUri);
  }

  @Override
  public CustomConversions customConversions() {
    return new CustomConversions(
        Lists.newArrayList(new LocalizedStringWriteConverter(), new LocalizedStringReadConverter(), new VersionReadConverter()));
  }

  @Override
  @Nullable
  protected UserCredentials getUserCredentials() {
    String username = propertyResolver.getProperty("username");
    String password = propertyResolver.getProperty("password");
    return isNullOrEmpty(username) || isNullOrEmpty(password) ? null : new UserCredentials(username, password);
  }

  @Override
  @Nullable
  protected String getAuthenticationDatabaseName() {
    return propertyResolver.getProperty("authSource");
  }

  @Nullable
  protected String getOptions() {
    return propertyResolver.getProperty("options");
  }

  public static class LocalizedStringWriteConverter implements Converter<LocalizedString, DBObject> {

    @Override
    public DBObject convert(LocalizedString source) {
      DBObject dbo = new BasicDBObject();
      source.entrySet().forEach(entry -> dbo.put(entry.getKey(), entry.getValue()));
      return dbo;
    }
  }

  public static class LocalizedStringReadConverter implements Converter<DBObject, LocalizedString> {

    @Override
    public LocalizedString convert(DBObject source) {
      LocalizedString rval = new LocalizedString();
      source.keySet()
          .forEach(key -> rval.put(key, source.get(key) == null ? null : source.get(key).toString()));
      return rval;
    }
  }


  private MongoClientURI buildUri() throws UnsupportedEncodingException {
    String utf8 = StandardCharsets.UTF_8.toString();
    Function<String, String> encode = (String s) -> {
      try {
        return URLEncoder.encode(s, utf8);
      } catch(UnsupportedEncodingException e) {
        log.error("Failed to encode " + e);
      }

      return null;
    };

    String url = propertyResolver.getProperty("url");
    String databaseName = encode.apply(propertyResolver.getProperty("databaseName"));

    if(isNullOrEmpty(url) || isNullOrEmpty(databaseName)) {
      log.error("Your MongoDB configuration is incorrect! The application cannot start. " +
        "Please check your Spring profile, current profiles are: {}", Arrays.toString(env.getActiveProfiles()));
      throw new ApplicationContextException("MongoDB is not configured correctly");
    }

    StringBuilder builder = new StringBuilder("mongodb://");
    StringBuilder optionsBuilder = new StringBuilder();

    UserCredentials userCredentials = getUserCredentials();
    String authSource = getAuthenticationDatabaseName();
    String options = getOptions();

    if (userCredentials != null && userCredentials.hasUsername() && userCredentials.hasPassword()) {
      builder.append(encode.apply(userCredentials.getUsername())).append(':')
        .append(encode.apply(userCredentials.getPassword())).append('@');
    }

    builder.append(url).append('/').append(databaseName);

    if (!isNullOrEmpty(authSource)) {
      optionsBuilder.append("authSource=").append(authSource);
    }

    if (!isNullOrEmpty(options)) {
      optionsBuilder.append('&').append(options);
    }

    if (optionsBuilder.length() > 0) {
      builder.append('?').append(optionsBuilder.toString());
    }

    return new MongoClientURI(builder.toString());
  }

  public static class VersionReadConverter implements Converter<DBObject, Version> {

    @Override
    public Version convert(DBObject dbObject) {
      return new Version((int)dbObject.get("major"), (int)dbObject.get("minor"), (int)dbObject.get("micro"), (String)dbObject.get("qualifier"));
    }
  }

}

