export interface StringMap {
  [key: string]: string | string[] | undefined;
}

export interface Message {
  msg: string;
  timestamp: number;
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
