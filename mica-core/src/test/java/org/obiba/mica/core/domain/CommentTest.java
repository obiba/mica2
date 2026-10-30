/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.HtmlUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CommentTest {

  // text typed by a user, with the characters that are encoded
  private static final String TYPED = "<b>&\"'é’ x@y =1+1";

  // as stored by all Mica versions (HtmlUtils.htmlEscape in Comment.Builder)
  private static final String STORED = "&lt;b&gt;&amp;&quot;&#39;&eacute;&rsquo; x@y =1+1";

  private static final List<String> TYPED_MESSAGES = List.of(
    TYPED,
    "A & B",
    "**markdown** with `code` and > quote\n\n- item 1\n- item 2",
    "<script>alert('x')</script>",
    "R&D, 50% & more, a < b",
    "emoji 😀 and 中文",
    "plain");

  @Test
  public void test_new_comment_is_encoded() {
    assertThat(newComment(TYPED).getMessage()).isEqualTo(STORED);
  }

  @Test
  public void test_new_comment_keeps_typed_entities() {
    // a new comment is stored as typed, a literal entity is not decoded
    assertThat(newComment("use &amp; or `&lt;br&gt;`").getMessage()).isEqualTo("use &amp;amp; or `&amp;lt;br&amp;gt;`");
  }

  @Test
  public void test_null_message_rejected() {
    // IllegalArgumentException as before, not a NullPointerException
    assertThatThrownBy(() -> newComment(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Comment.newBuilder(newComment(TYPED)).editedMessage(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void test_edit_stored_comment_unchanged() {
    // the editor sends the stored message back
    Comment comment = newComment(TYPED);
    for (int i = 0; i < 5; i++) {
      Comment.newBuilder(comment).editedMessage(comment.getMessage());
      assertThat(comment.getMessage()).isEqualTo(STORED);
    }
  }

  @Test
  public void test_edit_comment_stored_by_older_versions_unchanged() {
    Comment comment = storedComment(STORED);
    Comment.newBuilder(comment).editedMessage(comment.getMessage());
    assertThat(comment.getMessage()).isEqualTo(STORED);
  }

  @Test
  public void test_edit_comments_unchanged() {
    TYPED_MESSAGES.forEach(typed -> {
      Comment comment = newComment(typed);
      String stored = comment.getMessage();
      Comment.newBuilder(comment).editedMessage(stored);
      Comment.newBuilder(comment).editedMessage(comment.getMessage());
      assertThat(comment.getMessage()).as(typed).isEqualTo(stored);
    });
  }

  @Test
  public void test_edit_comment_with_new_text() {
    Comment comment = newComment(TYPED);
    Comment.newBuilder(comment).editedMessage("C & D");
    assertThat(comment.getMessage()).isEqualTo("C &amp; D");
  }

  @Test
  public void test_edit_stored_comment_with_added_text() {
    // the editor shows the stored message, new text is typed next to the entities
    Comment comment = newComment("A & B");
    Comment.newBuilder(comment).editedMessage(comment.getMessage() + " and C & D");
    assertThat(comment.getMessage()).isEqualTo("A &amp; B and C &amp; D");
  }

  @Test
  public void test_edit_comment_decodes_typed_entities() {
    // the known limit: on edit, a literal entity typed in the message is decoded
    Comment comment = newComment("A & B");
    Comment.newBuilder(comment).editedMessage("use &amp; here");
    assertThat(comment.getMessage()).isEqualTo("use &amp; here");
  }

  @Test
  public void test_comment_saved_as_json_string() {
    // older versions saved some messages as a json string
    assertThat(storedComment("&quot;line 1\\nline 2&quot;").getMessage()).isEqualTo("line 1\nline 2");
    assertThat(storedComment("&quot;He said \\&quot;hi\\&quot;\\nbye&quot;").getMessage()).isEqualTo("He said &quot;hi&quot;\nbye");
    assertThat(storedComment("&quot;C:\\\\temp\\tA &amp; B&quot;").getMessage()).isEqualTo("C:\\temp\tA &amp; B");
    assertThat(storedComment("&quot;caf\\u00e9\\r\\n&quot;").getMessage()).isEqualTo("caf&eacute;\r\n");
    assertThat(storedComment("&quot;a\\/b&lt;c&gt;&quot;").getMessage()).isEqualTo("a/b&lt;c&gt;");
  }

  @Test
  public void test_comment_saved_as_json_string_without_escape_unchanged() {
    // cannot be told apart from a message typed between double quotes
    assertThat(storedComment("&quot;hello world&quot;").getMessage()).isEqualTo("&quot;hello world&quot;");
  }

  @Test
  public void test_quoted_comment_unchanged() {
    List.of("\"hello\"", "\"a\" and \"b\"", "\"\"", "\"", "\"a \\q\"", "\"a \\u00zz\"", "\"a\\\"", "\"a\nb\"",
      "\"x\" \\n \"y\"", "\"open \\n only", "close \\n only\"").forEach(typed -> {
      Comment comment = newComment(typed);
      assertThat(comment.getMessage()).as(typed).isEqualTo(HtmlUtils.htmlEscape(typed));
    });
  }

  @Test
  public void test_edit_comment_saved_as_json_string() {
    // the editor sends the cleaned message back, it is saved as plain text
    Comment comment = storedComment("&quot;He said \\&quot;hi\\&quot;\\nbye&quot;");
    Comment.newBuilder(comment).editedMessage(comment.getMessage());
    assertThat(comment.getMessage()).isEqualTo("He said &quot;hi&quot;\nbye");
    Comment.newBuilder(comment).editedMessage(comment.getMessage());
    assertThat(comment.getMessage()).isEqualTo("He said &quot;hi&quot;\nbye");
  }

  //
  // Private methods
  //

  private Comment newComment(String message) {
    return Comment.newBuilder().message(message).build();
  }

  private Comment storedComment(String message) {
    Comment comment = new Comment();
    comment.setMessage(message);
    return comment;
  }
}
