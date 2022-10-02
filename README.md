# XsDiscordBotLoader

Copyright © 2022 IceXinShou. All rights reserved.

## API Usage:

Require Java 17

Create a class and make it `extends PluginEvent`

now, you can add these blow:

```java
  @Override
  public void initLoad() {}

  @Override
  public void unload() {}

  @Override
  public void loadConfigFile() {}

  @Override
  public void loadVariables() {}

  @Override
  public void loadLang() {}

  @Override
  public CommandData[] guildCommands() {}
  
  @Override
  public SubcommandData[] subGuildCommands() {}
  
  @Override
  public CommandData[] globalCommands() {}
```

For more examples, please visit plugins in Plugins folder 
