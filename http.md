# HTTP Endpoints

## Encryption test

Here is a simple test that makes sure you use the correct encryption method
and correct data format.

Password: `Hello, 你好` (there is only one space after the comma)
Plain: `This is a test. 这是一段测试。` (there is only one space after the dot)
Encrypted base64: `L29yV+20yIDL0re3mmXTxi8YUxSDwzGL73UvVPIBoPRM2c0lPlTpxkaWFHWQRJxKH1+Tbs3cof+/NjHJhCTtAzg=`

If your result is different, then you should fix it first.

## Request

All requests are json before encryption:
```json
{
  "request_id": "your id",
  "timestamp": 12345,
  "cmd": "your command",
  "params": [
    "list", "of", "params", "for", "command"
  ]
}
```

The `request_id` is a client thing, server will repeat this when responded.
It's recommended to use random UUID as it disturbers the encrypted cipher text.

The `timestamp` field is required to against MITM attack.
The timestamp is unix seconds.
Any request older than 30 seconds will be discarded (in server's view).
Make sure your time is correct.

The `cmd` is what you want to do, see below.
The `params` depends on `cmd` and can be anything (mostly null and string).

## Response

There are two types of response:
+ If something fundamentally goes wrong, the response will be unencrypted
  + for example, the request cannot be decrypted, or timestamp is invalid
  + under this situation, the client might be bad or misconfigured, and may not decrypt properly
+ If something logically goes wrong, the response will be encrypted
+ Non-error response will always be encrypted unless debug mode is enabled

After decrypted, the response looks like this:
```json
{
  "request_id": "request id",
  "err": "err message",
  "result": null
}
```

The `request_id` is what client sent to server.
The `err` will be `null` if no error, or the text message that describe the error.


The `result` can be anything, including `null`.
It depends on what `cmd` you sent.
When `err` is not null, the `result` is always `null` (You got nothing when error happens).

## Application models

There are two application related models.

### Metadata

HTTP endpoints use this json structure to represent a metadata:

```json
{
  "meta_name": "name",
  "meta_type": "TAG or TEXT",
  "meta_need_value": false,
  "meta_value": "some value"
}
```

None of those will be null. If `meta_need_value` is false, it means there is no
need to display value to user, the `meta_value` will be empty string.

The metadata here doesn't include its related entry id, since it will be included
as part of entry, or the caller know it when sending the request.

### Entry

HTTP endpoints use this json structure to represent a entry:

```json
{
  "entry_id": "id",
  "entry_type": "ITEM, BOX or LOCATION",
  "entry_parent_id": null,
  "entry_children_count": 3,
  "entry_name": "something",
  "entry_note": "note here",
  "entry_meta_list": [
    {
      "meta_name": "name",
      "meta_type": "TAG or TEXT",
      "meta_need_value": false,
      "meta_value": "some value"
    }
  ]
}
```

Most of those fields are fairly self-explanatory.
The `entry_children_count` shows how many children this entry has, for clients
to decide if it should show user a button to list children.

The `entry_meta_list` is a complete list of metadata, considering there will not
be a lot of metadata, it would not become a big chunk of data.

## Cmd

There are several commands that offer a set of similar operations from CLI.

### `browse`

Select all entries that under a given parent.

Params:
+ `parent_id`: a nullable string representing a parent id, must exist if not null.

Returns: list of entry id, string.

This offers the client a way to browse the database in a tree-like view.

### `search`

Select all entries that contain a set of keywords.

Params:
+ `keyword`: a non-null and non-blank string, representing the keyword you want to search
+ `keyword`: you can have multiple keywords, just place them in the `params`.

Return: list of entry id, string.

The program will search and return all entries that:
+ entry id contains keyword, or
+ parent entry id contains keyword, or
+ name contains keyword, or
+ note contains keyword, or
+ meta's name contains keyword, or
+ meta's value contains keyword

The search is case-insensitive and the returned value is unique.

### `view`

Select an entry with the given id.

Params:
+ `entry_id`: a non-null string representing an entry, must exist.

Returns: Entry.

This will return a detailed object for the requested entry id.

### `create_entry`

Create a new entry.

Params:
+ `entryId`: a non-null string for entry id, must not exist
+ `type`: a nullable string for the type, must be `ITEM`, `BOX` or `LOCATION` if not null.
  + when `null`, it will try to infer the type if your id follows the `IBL` prefix.
+ `parent_id`: a nullable string for parent id, must exist if not null
+ `name`: a non-null string for name, leave it empty if not used
+ `note`: a non-null string for note, leave it empty if not used

Returns: the Entry you just created.

### `update_entry`

Update a entry

Params:
+ `entryId`: a non-null string for entry id, which must exist
+ `field_name`: a non-null string for what field to update
+ `value`: a non-null or nullable string for value of the the field you want to update
+ `field_name`: you can have multiple fields updated in one request
+ `value`: just make sure the field name and value are in pair

Field name and value:
+ `parent_id`: nullable string
+ `name`: non-null string, can be empty
+ `note`: non-null string, can be empty

Returns: the newly updated Entry

### `delete_entry`

Delete an entry including its metadata.

Params:
+ `entryId`: which entry you want to delete, must be exist

Returns: the deleted Entry, this is your last chance to see it.

You may undo the deleting by recreating the entry and metadata with what returned to you. 

### `create_meta`

Create a new metadata.

Params:
+ `entryId`: a non-null string for entry id, must exist
+ `name`: a non-null string for name, which can't be empty.
+ `type`: a non-null string for the type, must be `TAG` or `TEXT`.
+ `value`: a non-null string for value, leave it empty if not used

Returns: the entry contains all metadata including the one you just created.

Note: based on `type`, the `value` might be ignored. For example, `TAG`.

### `update_meta`

Update a metadata

Params:
+ `entryId`: a non-null string for entry id, which must exist
+ `name`: a non-null string for metadata, which must exist
+ `field_name`: a non-null string for what field to update
+ `value`: a non-null or nullable string for value of the the field you want to update
+ `field_name`: you can have multiple fields updated in one request
+ `value`: just make sure the field name and value are in pair

Field name and value:
+ `type`: non-null string, must be `TAG` or `TEXT`
+ `value`: non-null string, can be empty

Returns: the related Entry including the newly updated metadata

Note: when set type to those doesn't need value, the `value` field will be reset to empty.
The value will be lost forever.

I suggest don't allow user to change the type.

### `delete_meta`

Delete an entry including its metadata.

Params:
+ `entryId`: which entry you want to delete, must be exist
+ `name`: which metadata you want to delete, must be exist

Returns: the deleted metadata, this is your last chance to see it.

You may undo the deleting by recreating the metadata with what returned to you.
