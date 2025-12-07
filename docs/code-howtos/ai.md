- ai package organization
- gui / logic / model
- how to make ui -> view view-model pattern
- currents
- task organization
- generic classess
- long task and shutdown signal
- ensure to use ChatModel from jabref


how to use MVStores:
general dissection:
1. MVStore - for different repositories. 
2. MVMap - (for entry chats) - one for "databasePath + entryID".
3. Entry - specific chunks and chathistory records with their ID (UUID).
Messages/Chunks should be distinct/separate/atomic. E.g. we don't store a List of messages in an entry.

I would like a forth layer: to distrinct database and entry, but it is what it is.

try to group repositories for one type of data. No: entry and group chat history repostiory, yes: single chat history repository.


explain how messages are stored (v1 and v2).



logical flaw in task aggregation: yoou need to add status listeners BEFORE executing.
