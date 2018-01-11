/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.runtime.Version;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories("org.obiba.mica")
public class MongoDbConfiguration {

  @Bean
  public CustomConversions customConversions() {
    return new CustomConversions(
        Lists.newArrayList(new LocalizedStringWriteConverter(), new LocalizedStringReadConverter(), new VersionReadConverter()));
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

  public static class VersionReadConverter implements Converter<DBObject, Version> {

    @Override
    public Version convert(DBObject dbObject) {
      return new Version((int)dbObject.get("major"), (int)dbObject.get("minor"), (int)dbObject.get("micro"), (String)dbObject.get("qualifier"));
    }
  }

}

