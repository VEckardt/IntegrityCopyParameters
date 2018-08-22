# IntegrityCopyParameters
This utility allows you to copy parameters and parameter values from one item to another.

# Hint
This is a 0.1 version, so very simple, no validation, just the copy operation.

# Important
This copy operation will overwrite all data at the target item!!!

# How to Use

java -jar IntegrityCopyParameters.jar --sourceItemID=101 --targetItemID=5593  --copyFields="Parameters,Parameter Values"

```
Integrity Copy Parameters - V0.1
----------------------------
Reading Field Values 'Parameters,Parameter Values' from Item '101' ...
Writing Field Values 'Parameters,Parameter Values' to Item '5593' ...
Field Values 'Parameters,Parameter Values' written.
```

# Possible Result

![CopyParamsResult](doc/CopyParamsResult.PNG)

# Tested with:
- Integrity 11.0
