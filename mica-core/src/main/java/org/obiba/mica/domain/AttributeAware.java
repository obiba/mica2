package org.obiba.mica.domain;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public interface AttributeAware {

  Multimap<String, Attribute> getAttributes();

  void setAttributes(LinkedListMultimap<String, Attribute> attributes);

  void addAttribute(Attribute attribute);

  void removeAttribute(Attribute attribute);

  void removeAllAttributes();

  boolean hasAttribute(String attName, @Nullable String namespace);

  List<Attribute> getAttributes(String attName, @Nullable String namespace);

  boolean hasAttribute(String attName, @Nullable String namespace, @Nullable Locale locale);

  Attribute getAttribute(String attName, @Nullable String namespace, @Nullable Locale locale) throws NoSuchAttributeException;
}
