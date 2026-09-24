/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.core.domain.SchemaFormContentAware;
import org.obiba.mica.file.FileRuntimeException;
import org.obiba.mica.file.FileStoreService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SchemaFormContentFileServiceTest {

  @InjectMocks
  private SchemaFormContentFileService schemaFormContentFileService;

  @Mock
  private FileStoreService fileStoreService;

  @Test
  public void get_file_entries__null_content__returns_empty() {
    Map<String, String> entries = schemaFormContentFileService.getFileEntries(entity(null));

    assertThat(entries.isEmpty(), is(true));
  }

  @Test
  public void get_file_entries__content_without_files__returns_empty() {
    Map<String, String> entries = schemaFormContentFileService.getFileEntries(entity("{\"a\":\"b\"}"));

    assertThat(entries.isEmpty(), is(true));
  }

  @Test
  public void get_file_entries__files_in_several_fields__returns_all_in_order() {
    String content = "{"
      + "\"field1\":{\"obibaFiles\":[{\"id\":\"f1\",\"fileName\":\"a.pdf\"}]},"
      + "\"field2\":{\"obibaFiles\":[{\"id\":\"f2\",\"fileName\":\"b.pdf\"},{\"id\":\"f3\",\"fileName\":\"c.pdf\"}]}"
      + "}";

    Map<String, String> entries = schemaFormContentFileService.getFileEntries(entity(content));

    assertThat(entries.size(), is(3));
    assertThat(entries.get("a.pdf"), is("f1"));
    assertThat(entries.get("b.pdf"), is("f2"));
    assertThat(entries.get("c.pdf"), is("f3"));
  }

  @Test
  public void get_file_entries__duplicate_names__appends_id_before_extension() {
    String content = "{\"field1\":{\"obibaFiles\":["
      + "{\"id\":\"f1\",\"fileName\":\"report.pdf\"},"
      + "{\"id\":\"f2\",\"fileName\":\"report.pdf\"},"
      + "{\"id\":\"f3\",\"fileName\":\"README\"},"
      + "{\"id\":\"f4\",\"fileName\":\"README\"}"
      + "]}}";

    Map<String, String> entries = schemaFormContentFileService.getFileEntries(entity(content));

    assertThat(entries.keySet(), contains("report.pdf", "report_f2.pdf", "README", "README_f4"));
  }

  @Test
  public void get_file_entries__path_in_name__keeps_base_name_only() {
    String content = "{\"field1\":{\"obibaFiles\":["
      + "{\"id\":\"f1\",\"fileName\":\"../../evil.sh\"},"
      + "{\"id\":\"f2\",\"fileName\":\"dir\\\\x.pdf\"},"
      + "{\"id\":\"f3\",\"fileName\":\"..\"}"
      + "]}}";

    Map<String, String> entries = schemaFormContentFileService.getFileEntries(entity(content));

    assertThat(entries.keySet(), contains("evil.sh", "x.pdf", "f3"));
  }

  @Test
  public void get_file_entries__missing_file_name__uses_id() {
    String content = "{\"field1\":{\"obibaFiles\":[{\"id\":\"f1\"}]}}";

    Map<String, String> entries = schemaFormContentFileService.getFileEntries(entity(content));

    assertThat(entries.keySet(), contains("f1"));
    assertThat(entries.get("f1"), is("f1"));
  }

  @Test
  public void get_file_entries__opens_no_stream() {
    String content = "{\"field1\":{\"obibaFiles\":[{\"id\":\"f1\",\"fileName\":\"a.pdf\"}]}}";

    schemaFormContentFileService.getFileEntries(entity(content));

    verifyNoInteractions(fileStoreService);
  }

  @Test
  public void write_zip__writes_one_entry_per_file_with_content() throws Exception {
    String content = "{\"field1\":{\"obibaFiles\":["
      + "{\"id\":\"f1\",\"fileName\":\"a.pdf\"},"
      + "{\"id\":\"f2\",\"fileName\":\"b.pdf\"}"
      + "]}}";
    when(fileStoreService.getFile("f1")).thenReturn(stream("content-a"));
    when(fileStoreService.getFile("f2")).thenReturn(stream("content-b"));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    schemaFormContentFileService.writeZip(schemaFormContentFileService.getFileEntries(entity(content)), output);

    Map<String, String> zipContent = readZip(output);
    assertThat(zipContent.keySet(), contains("a.pdf", "b.pdf"));
    assertThat(zipContent.get("a.pdf"), is("content-a"));
    assertThat(zipContent.get("b.pdf"), is("content-b"));
  }

  @Test
  public void write_zip__missing_file__is_skipped_others_written_and_listed() throws Exception {
    String content = "{\"field1\":{\"obibaFiles\":["
      + "{\"id\":\"f1\",\"fileName\":\"a.pdf\"},"
      + "{\"id\":\"f2\",\"fileName\":\"b.pdf\"},"
      + "{\"id\":\"f3\",\"fileName\":\"c.pdf\"}"
      + "]}}";
    when(fileStoreService.getFile("f1")).thenReturn(stream("content-a"));
    when(fileStoreService.getFile("f2")).thenThrow(new FileRuntimeException("f2"));
    when(fileStoreService.getFile("f3")).thenReturn(stream("content-c"));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    schemaFormContentFileService.writeZip(schemaFormContentFileService.getFileEntries(entity(content)), output);

    Map<String, String> zipContent = readZip(output);
    assertThat(zipContent.keySet(), contains("a.pdf", "c.pdf", "MISSING.txt"));
    assertThat(zipContent.get("MISSING.txt"), containsString("b.pdf"));
  }

  @Test
  public void write_zip__missing_file_with_existing_missing_txt__does_not_overwrite_it() throws Exception {
    String content = "{\"field1\":{\"obibaFiles\":["
      + "{\"id\":\"f1\",\"fileName\":\"MISSING.txt\"},"
      + "{\"id\":\"f2\",\"fileName\":\"b.pdf\"}"
      + "]}}";
    when(fileStoreService.getFile("f1")).thenReturn(stream("user-file"));
    when(fileStoreService.getFile("f2")).thenThrow(new FileRuntimeException("f2"));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    schemaFormContentFileService.writeZip(schemaFormContentFileService.getFileEntries(entity(content)), output);

    Map<String, String> zipContent = readZip(output);
    assertThat(zipContent.keySet(), contains("MISSING.txt", "_MISSING.txt"));
    assertThat(zipContent.get("MISSING.txt"), is("user-file"));
    assertThat(zipContent.get("_MISSING.txt"), containsString("b.pdf"));
  }

  @Test
  public void write_zip__all_files_retrieved__no_missing_entry() throws Exception {
    String content = "{\"field1\":{\"obibaFiles\":[{\"id\":\"f1\",\"fileName\":\"a.pdf\"}]}}";
    when(fileStoreService.getFile("f1")).thenReturn(stream("content-a"));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    schemaFormContentFileService.writeZip(schemaFormContentFileService.getFileEntries(entity(content)), output);

    assertThat(readZip(output).keySet(), contains("a.pdf"));
  }

  @Test
  public void write_zip__no_files__produces_valid_empty_zip() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    schemaFormContentFileService.writeZip(schemaFormContentFileService.getFileEntries(entity(null)), output);

    Map<String, String> zipContent = readZip(output);
    assertThat(zipContent.isEmpty(), is(true));
  }

  @Test
  public void save__does_not_open_file_streams() {
    String content = "{\"field1\":{\"obibaFiles\":[{\"id\":\"f1\",\"fileName\":\"a.pdf\"}]}}";

    schemaFormContentFileService.save(entity(content), Optional.empty(), "/data-access-request/x");

    verify(fileStoreService, never()).getFile(any());
  }

  private SchemaFormContentAware entity(String content) {
    return new SchemaFormContentAwareStub(content);
  }

  private ByteArrayInputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  private Map<String, String> readZip(ByteArrayOutputStream output) throws Exception {
    Map<String, String> entries = new LinkedHashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        zis.transferTo(content);
        entries.put(entry.getName(), content.toString(StandardCharsets.UTF_8));
      }
    }
    return entries;
  }

  private static class SchemaFormContentAwareStub implements SchemaFormContentAware {
    private String content;

    SchemaFormContentAwareStub(String content) {
      this.content = content;
    }

    @Override
    public String getContent() {
      return content;
    }

    @Override
    public void setContent(String value) {
      this.content = value;
    }
  }
}
