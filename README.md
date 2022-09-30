# XsDiscordBotLoader

## API Usage:

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

Example: https://github.com/IceLeiYu/DC_Ban_Plugin