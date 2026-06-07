# Privacy Policy

**Effective date:** 30 May 2026

This policy explains what data Kazka collects, why, and what you can do about it.

## 1. Data controller

The controller of your personal data is the Ukrainian sole entrepreneur (ФОП) **Dmytro Makarenko** (Ukrainian tax ID РНОКПП: _to be provided_), address: **Vinnytsia, Ukraine**. Privacy questions: `d.makarenko.home@gmail.com`.

## 2. What we collect

**Account data**

- Email, display name.
- Password hash (we never store the password in plain text).
- If you sign in with Google or Apple — a stable identifier from that service and the email it shares.
- Email-verification state, account suspension state.

**Child profiles**

- Child's name, age band, interests, preferred language.
- An image, if you added one to the profile.

**Generated content**

- The text and illustrations of tales you create.
- Saved recurring characters (consistent personalities across tales).
- Bedtime ritual settings: time, themes, enabled/disabled.

**Usage data**

- How many tales you've created this month (used for the free-tier limit).
- Last login time.
- Technical request logs (IP, user-agent) for security and error diagnostics.

**Payment data**

We **don't** store the full card number. Payment is handled by one of the providers — Apple, Monobank Acquiring. Monobank Acquiring stores the card token securely on its side; we keep only the `walletId` and `cardToken` values needed to charge again. We never see the full card number. On our side we also keep an entitlement record: which plan is active, until when, via which provider.

**Push notifications (mobile app only)**

If you enabled push, we store a device token so we can deliver bedtime reminders.

## 3. Children's data

- Adults create accounts. Children don't register themselves on Kazka.
- The child's name, age band, and interests are entered by the adult and are used only to personalise generated tales.

## 4. Legal basis for processing (GDPR Art. 6)

- **Performance of a contract** — to deliver the service (generate tales, process payments, send verification emails).
- **Legitimate interest** — content moderation, abuse prevention, basic error diagnostics.
- **Consent** — any marketing communication (we currently don't send any; if we start, we'll ask explicitly).

## 5. Who we share data with (sub-processors)

| Provider | What it sees | Jurisdiction |
|---|---|---|
| Google (Gemini API) | Your prompt text and the generated tale text (no user identifier attached) — used for tale generation, editing, scene extraction, and moderation | USA |
| Fal.ai | The image scene description (derived from your prompt; no user identifier attached) — used for illustration generation | USA |
| Monobank Acquiring | Email, payment amount, card token | Ukraine |
| Apple App Store | Apple ID, payment amount | USA |
| Google Sign-In | Your Google identifier, email | USA |
| Apple Sign-In | Your Apple identifier, email (or relay) | USA |
| Cloudflare | IP, request metadata (DNS, proxy) | USA |
| Google (Gmail SMTP) | Email, contents of verification / system emails | USA |
| Hetzner | Server logs (hosting) | Germany |

## 6. International transfers

Some of these providers operate outside Ukraine and the EU. Transfers are protected by Standard Contractual Clauses or equivalent safeguards.

## 7. Retention

- Account data — for as long as the account is active + 30 days after a deletion request.
- Generated tales — for as long as the account is active; you can delete individual tales from the archive.
- Payment records — 3 years, as required by Ukrainian tax law.
- Technical logs — 90 days.

## 8. Your rights

You have the right to:

- **Access** — receive a copy of your data.
- **Rectification** — correct inaccurate data.
- **Erasure** — delete the account and related data.
- **Portability** — receive your data in a machine-readable format.
- **Withdraw consent** — where processing relies on consent.
- **Complaint** — file with the Ukrainian Parliament Commissioner for Human Rights (Ukraine's data-protection authority); if you live in the EU — your national data-protection authority.

To exercise any of these, email `d.makarenko.home@gmail.com`. We respond within **30 days**.

## 9. Cookies and local storage

Your browser stores:

- Authentication tokens (so you don't have to sign in each visit).
- Your interface language.
- Your theme preference (light / dark).

These are **strictly necessary** for the site to work and don't require separate consent.

Analytics: we don't use any third-party analytics tools.

## 10. Security

- Passwords are stored as bcrypt hashes, never in plain text.
- All connections use HTTPS.
- Requests that attempt to generate harmful content are blocked by a moderation pipeline; repeated attempts lead to account suspension.
- If a data breach happens that could harm you, we'll notify you and the relevant authorities within the timeframes required by law.

## 11. Parental responsibility

Kazka is intended for adults who create tales for their children. Parents and legal guardians are solely responsible for supervising any minor's use of the service and for any content generated under their account. The platform is not responsible for the child or for how the account holder uses the service in the child's presence.

## 12. Contact and changes to this policy

Questions, requests, complaints — `d.makarenko.home@gmail.com`. If we make material changes to this policy, we'll give at least 14 days' notice via the account email.
