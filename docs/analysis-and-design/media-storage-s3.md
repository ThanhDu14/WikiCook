# Uploaded media on S3 + CloudFront

Avatars (spec 011) and Cooksnap photos (spec 070) go through one store, `com.wikicook.common.media.MediaStorage`:

| `WIKICOOK_MEDIA_STORAGE` | Store | Used by |
| :--- | :--- | :--- |
| `local` (default) | `LocalDiskMediaStorage`, directory `WIKICOOK_MEDIA_DIR` (compose: volume `media`) | development, tests, CI |
| `s3` | `S3MediaStorage`, a private bucket | deployment |

The database keeps only the object key. Recipe images from the scraper (`recipe_media.external_url`) are not touched.

| What | Column | Object key |
| :--- | :--- | :--- |
| Avatar | `user_profiles.avatar_storage_key` = `avatars/<uuid>.jpg` | the same |
| Cooksnap | `cooksnaps.storage_key` = `cs_<recipe>_<time>_<id>.webp` | `cooksnaps/<key>`, thumbnail `cooksnaps/thumb_<key>` |
| Hidden Cooksnap (moderation) | unchanged | moved to `hidden/cooksnaps/...`, back on restore |

## URLs given to clients

- `WIKICOOK_MEDIA_PUBLIC_BASE_URL` blank: `/api/v1/media/avatars/<uuid>.jpg` and `/api/v1/media/cooksnaps/<key>[?thumbnail=true]`.
  The backend reads the object and checks that a Cooksnap is `VISIBLE`.
- `WIKICOOK_MEDIA_PUBLIC_BASE_URL=https://dxxxx.cloudfront.net`: `<base>/avatars/<uuid>.jpg`,
  `<base>/cooksnaps/<key>` and `<base>/cooksnaps/thumb_<key>`, served by CloudFront from the bucket.

When a Cooksnap or its review is hidden, its objects leave `cooksnaps/` after the transaction commits. CloudFront
stops serving the photo when its cached copy expires. Cooksnap objects are stored with `Cache-Control: public, max-age=3600`,
so this takes at most one hour, or straight away after an invalidation of `/cooksnaps/<key>*`. Avatar keys never change,
so their objects are stored with `max-age=31536000, immutable`.

## AWS setup (region `ap-southeast-1`)

1. **Bucket** `wikicook-media-<team>`: keep *Block all public access* on and leave versioning off.
2. **CloudFront distribution**: origin = the bucket, using *Origin Access Control (OAC)*. Viewer protocol is *Redirect HTTP to HTTPS*.
   Cache policy is *CachingOptimized*, which honours the object `Cache-Control`.
3. **Bucket policy**: CloudFront may read **only** `avatars/*` and `cooksnaps/*`. `hidden/*` stays unreachable (403):

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Sid": "CloudFrontReadsPublicPrefixesOnly",
       "Effect": "Allow",
       "Principal": { "Service": "cloudfront.amazonaws.com" },
       "Action": "s3:GetObject",
       "Resource": [
         "arn:aws:s3:::wikicook-media-<team>/avatars/*",
         "arn:aws:s3:::wikicook-media-<team>/cooksnaps/*"
       ],
       "Condition": { "StringEquals": { "AWS:SourceArn": "arn:aws:cloudfront::<account-id>:distribution/<distribution-id>" } }
     }]
   }
   ```

4. **Permissions of the app**: hiding a Cooksnap uses `CopyObject`, which needs `GetObject` and `PutObject`. The app needs nothing else:

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Effect": "Allow",
       "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
       "Resource": "arn:aws:s3:::wikicook-media-<team>/*"
     }]
   }
   ```

   - On **EC2**: attach the policy to an IAM role and give the instance that role (instance profile). Leave `AWS_ACCESS_KEY_ID` empty.
   - Elsewhere (a laptop, a VPS outside AWS): create an IAM user with only this policy and put its access key in `src/backend/.env`.
     `.env` is git-ignored; never commit a key.

5. **Budget alert**: AWS Budgets, for example 1 USD per month.

## `src/backend/.env` for compose

```dotenv
WIKICOOK_MEDIA_STORAGE=s3
WIKICOOK_MEDIA_S3_BUCKET=wikicook-media-<team>
WIKICOOK_MEDIA_S3_REGION=ap-southeast-1
WIKICOOK_MEDIA_PUBLIC_BASE_URL=https://dxxxx.cloudfront.net
# only outside AWS:
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```

Credentials are read by the AWS SDK default chain (environment, then `~/.aws`, then the IAM role). They are never
part of `application.yml`.

## Existing files

Files already in the `media` volume are not copied to S3 by the application. To keep them, copy them once with the
same keys before switching to `s3`:

```bash
docker compose cp app:/var/lib/wikicook/media ./media-export
aws s3 sync ./media-export/avatars   s3://wikicook-media-<team>/avatars
aws s3 sync ./media-export/cooksnaps s3://wikicook-media-<team>/cooksnaps
```

Cooksnaps saved before this change by a backend running in compose failed to write (`/app` is not writable). A
backend run outside compose saved them under `uploads/cooksnaps/` of its working directory. Sync that folder to
`cooksnaps/` if it has any.

## Tests

- `S3MediaStorageTest`: the requests sent to a mocked `S3Client` (bucket, key, MIME type, cache policy).
- `S3MediaStorageIT`: the store, the Spring wiring (`storage=s3`) and the Cooksnap hide/restore against a real S3 API
  (Adobe S3Mock in Docker).
- `StorageServiceTest`: the Cooksnap keys, hide and restore on the local store.
