import type { UserProfileDto } from 'src/models/Mica';

/** the value of a profile attribute (`firstName`, `lastName`, `email`...) */
export function getProfileAttribute(profile: UserProfileDto | undefined, key: string): string | undefined {
  return profile?.attributes?.find((attribute) => attribute.key === key)?.value || undefined;
}

/** "First Last" when both names are known, the username otherwise */
export function getUserDisplayName(profile: UserProfileDto | undefined, username?: string): string {
  const firstName = getProfileAttribute(profile, 'firstName');
  const lastName = getProfileAttribute(profile, 'lastName');
  return firstName && lastName ? `${firstName} ${lastName}` : (profile?.username ?? username ?? '');
}
