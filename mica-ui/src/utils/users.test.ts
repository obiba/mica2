import { describe, expect, it } from 'vitest';
import { getProfileAttribute, getUserDisplayName } from './users';

const profile = {
  username: 'jdoe',
  groups: [],
  attributes: [
    { key: 'firstName', value: 'John' },
    { key: 'lastName', value: 'Doe' },
    { key: 'email', value: '' },
  ],
};

describe('users', () => {
  it('reads a profile attribute, empty as undefined', () => {
    expect(getProfileAttribute(profile, 'firstName')).toBe('John');
    expect(getProfileAttribute(profile, 'email')).toBeUndefined();
    expect(getProfileAttribute(undefined, 'firstName')).toBeUndefined();
  });

  it('displays the full name when complete, the username otherwise', () => {
    expect(getUserDisplayName(profile)).toBe('John Doe');
    expect(getUserDisplayName({ ...profile, attributes: [{ key: 'firstName', value: 'John' }] })).toBe('jdoe');
    expect(getUserDisplayName(undefined, 'admin')).toBe('admin');
    expect(getUserDisplayName(undefined)).toBe('');
  });
});
