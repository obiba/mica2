package org.obiba.mica.file;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.obiba.mica.domain.AbstractGitPersistable;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AttachmentSerialize {

  Class<? extends AttachmentSerializer<? extends AbstractGitPersistable>> value();

}
