# jim-cli
JVM based Inventory Management for Personal usage, purely in CLI. (Powered by Kotlin/JVM)

This is an alternative solution for using Memento Database as my Inventory Management system,
as part of my belt-tightening policy during I'm unemployed.

Currently there are two types of interface: CLI and HTTP.
I was trying to implement TUI, but solely rely on JVM is not possible (in an elegant and not wonky way).

# Function

This is a local system that stores data in SQLite.
By default, the database will be backup by [the script](https://github.com/hurui200320/lto-tar-backup-script).

There are two types of data: Entry and Metadata.

## Entry

An entry is a record of physically existed object, it can be:
+ Location: A location, or a sub location in a location, like a kitchen in one of your houses
+ Box: A box, in a location or another box
+ Item: An item, in a box or a location

All entries share the same data structure, only the type makes them different.
An entry has the following fields:
+ Entry id
+ Entry type
+ Entry name
+ note
+ Parent entry id

Name and note are optional, which means to help you identify your items and box.
Parent entry id records its father, representing a tree-like relationship,
which is pretty natural considering you will put an item in one container (box or location),
and can only be placed in at most one container.

An entry is indexed by its id, which is not case-sensitive (stored as uppercase in the database).
There is no requirement for id, but here is some advice that may help you
organize your items:

+ use prefix to distinguish the type, not for program, but for you
  + I personally use `I` for items, `B` for boxes and `L` for locations
+ I use A-Z and 0–9, but not `0` and `O`, `1` and `I`
+ I use 10 digits in total, long enough, but not too long to type
+ I use barcode printer and android phone to make things easier
  + I print the id along with the barcode, in case the barcode is unreadable
  + I also print multiple times to make sure every side of the box has a barcode
+ Item is not the smallest thing in the system
  + For tiny objects, I just list them in the box's note instead of sticking barcode to them

## Metadata

Sometimes there are data that don't fit in the name or note,
but still describe your entries.
I call it metadata.
A metadata has the following fields:
+ Entry id
+ Meta name
+ Meta value
+ Meta type

Each entry is indexed by its related entry id and the name.
The name is case-sensitive, but recommended to be lowercase
and use `_` instead of space.

Metadata currently only has two types:
+ Tag: Just a name, value is not important.
+ Text: A name and a block of text stored in value.

You may use metadata as your wish, or don't use metadata at all:
Just write everything in the entry's note.

I'd prefer use metadata to mark an item, for example,
with tag `lithium_battery`, I know an item or a box contain battery
which should not be exposed to heat or cold, which means I have
to store them in-door to avoid heat in summer and cold in winter.

I also use text metadata to describe the appearance of item and box,
for example, if I know what I need is a red box, it would be much easier
to spot it.


# Usage

There are two interfaces right now: CLI and HTTP.

## CLI

CLI offers a simple yet functional interface to manage your items.
You can always type `-h` to see the help of a given command.

Note: based on how clikt works, it's not possible to parse multiple lines
of text from cli command, so there is a `--multiline-something` flag that will
ask your multiline text from stdin, just type your content and use `Ctrl+Z` or `Ctrl+D`
to send EOF to end it.

I'll not repeat what I wrote in the help string, sometimes the code will give you
a clearer answer than natural language.
**But if you still confused, feel free to ask in issue area.** 

## HTTP

HTTP interface is a temporary way to allow other programs to talk with this program
efficiently.
By design, this interface will only offer HTTP service in a private home network
in a short time, so the security might be weak.
To against the Man In The Middle attack, I designed a post-based encrypted endpoint
to process request.

All requests are sent to the `/` endpoint using `POST`, with AES-256-GCM encrypted body.
The encryption key is configured by `jim server <password>`, the password's `SHA-256` hash
will be the encryption key.
Encryption is `AES/GCM/NoPadding` with 12 bytes of IV and 128 bits of authentication.

The encrypted byte data are directly used as POST payload.
The first 12 bytes are IV, then the output of `cipher.doFinal`.
With option `-d`, in debug mode, the server accepts non-encrypted json payload, for debug only.

When encrypting, json text is encoded using UTF-8 charset. I think it's common knowledge. 

See [http.mc](./http.md) for detailed endpoint info.

# License

As it's my personal project, it's AGPLv3.
