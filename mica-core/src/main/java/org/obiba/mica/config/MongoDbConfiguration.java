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

import org.bson.Document;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.runtime.Version;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "org.obiba.mica")
public class MongoDbConfiguration {

  @Bean
  public MongoCustomConversions mongoCustomConversions() {
    return new MongoCustomConversions(
        Lists.newArrayList(new LocalizedStringWriteConverter(), new LocalizedStringReadConverter(), new VersionReadConverter()));
  }

  public static class LocalizedStringWriteConverter implements Converter<LocalizedString, Document> {

    @Override
    public Document convert(LocalizedString source) {
      Document dbo = new Document();
      source.entrySet().forEach(entry -> dbo.put(entry.getKey(), entry.getValue()));
      return dbo;
    }
  }

  public static class LocalizedStringReadConverter implements Converter<Document, LocalizedString> {

    @Override
    public LocalizedString convert(Document source) {
      LocalizedString rval = new LocalizedString();
      source.keySet()
          .forEach(key -> rval.put(key, source.get(key) == null ? null : source.get(key).toString()));
      return rval;
    }
  }

  public static class VersionReadConverter implements Converter<Document, Version> {

    @Override
    public Version convert(Document dbObject) {
      return new Version((int)dbObject.get("major"), (int)dbObject.get("minor"), (int)dbObject.get("micro"), (String)dbObject.get("qualifier"));
    }
  }

}

