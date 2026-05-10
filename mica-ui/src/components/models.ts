export interface StringMap {
  [key: string]: string | string[] | undefined;
}

export interface Message {
  msg: string;
  timestamp: number;
}

export interface FileObject extends Blob {
  readonly size: number;
  readonly name: string;
  readonly path: string;
  readonly type: string;
}

export interface EnumOption {
  key: string;
  title: string;
}

export interface SchemaFormField {
  key: string;
  type: string;
  format?: string;
  title?: string;
  description?: string;
  default?: string;
  minimum?: number;
  maximum?: number;
  fileFormats?: string[];
  enum?: EnumOption[];
  items: SchemaFormField[];
}

export interface SchemaFormObject {
  $schema: string;
  type: string;
  title?: string;
  description?: string;
  items: SchemaFormField[];
  required: string[];
}

export interface FormObject {
  [key: string]: boolean | number | string | FileObject | FormObject | Array<FormObject> | undefined;
}

export const DefaultAlignment: 'left' | 'right' | 'center' = 'left';

export interface RealmForms {
  'agate-ad-realm': string;
  'agate-jdbc-realm': string;
  'agate-ldap-realm': string;
  'agate-oidc-realm': string;
  form: string;
  userInfoMapping: string;
  userInfoMappingDefaults: {
    'agate-oidc-realm': {
      [key: string]: string;
    };
  };
}

export interface OIDCRealmConfig {
  clientId: string;
  secret: string;
  discoveryURI: string;
  scope?: string;
  useNonce?: boolean;
  connectTimeout?: number;
  readTimeout?: number;
  providerUrl?: string;
  groupsClaim?: string;
  groupsJS?: string;
  prompt?: string | undefined;
  maxAge?: number | undefined;
}

export interface LDAPRealmConfig {
  url: string;
  systemUsername: string;
  systemPassword: string;
  userDnTemplate: string;
}

export interface ADRealmConfig {
  url: string;
  systemUsername: string;
  systemPassword: string;
  searchFilter: string;
  searchBase?: string;
  principalSuffix?: string;
}

export interface JDBCRealmConfig {
  url: string;
  username: string;
  password: string;
  authenticationQuery: string;
  saltStyle?: string;
  externalSalt?: string;
  algorithmName?: string;
}
