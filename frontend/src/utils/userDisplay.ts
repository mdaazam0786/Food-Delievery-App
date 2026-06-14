import type { UserProfile } from '../services/userService';

/** First token of fullName — null when unavailable. */
export function resolveFirstName(profile: UserProfile | null | undefined): string | null {
  if (!profile) return null;

  const fromFull = profile.fullName?.trim().split(/\s+/).find(Boolean);
  if (fromFull) return fromFull;

  return null;
}

/** Category slider greeting — personalized or generic fallback. */
export function categoryGreeting(
  profile: UserProfile | null | undefined,
  loading: boolean,
): string {
  if (loading) return "What's on your mind?";

  const firstName = resolveFirstName(profile);
  if (firstName) return `${firstName}, what's on your mind?`;

  return "What's on your mind?";
}
