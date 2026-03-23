# SlimeWorldManager - Multi-Database Edition

<div align="center">

![Java](https://img.shields.io/badge/Java-8+-orange.svg)
![Spigot](https://img.shields.io/badge/Spigot-1.8.8--1.15.2-yellow.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**Sistema avançado de gerenciamento de mundos usando Slime Region Format (SRF)**

[Instalação](#-instalação) • [Configuração](#️-configuração) • [Comandos](#-comandos) • [API](#-api-para-desenvolvedores)

</div>

---

## 🚀 Sobre o Projeto

O SlimeWorldManager implementa o **Slime Region Format (SRF)**, um formato de armazenamento de mundos desenvolvido pela equipe do Hypixel para carregar e salvar mundos de forma extremamente eficiente.

### Por que usar SWM?

- **Carregamento instantâneo**: Mundos carregam em milissegundos ao invés de segundos
- **Economia de disco**: Mundos são armazenados de forma compactada
- **Clonagem dinâmica**: Perfeito para minigames (BedWars, SkyWars, Duels)
- **Sem lag de I/O**: Mundos ficam em memória durante o uso
- **Multi-instância**: Suporte a múltiplos servidores compartilhando mundos

### ✨ Diferenciais desta Versão

✅ **Suporte a 3 bancos de dados**: MySQL, MariaDB e PostgreSQL  
✅ **Pool de conexões otimizado**: HikariCP com configurações ajustáveis  
✅ **Sistema de locks robusto**: Previne corrupção em ambientes multi-servidor  
✅ **Carregamento assíncrono**: Não trava o servidor durante geração de mundos  
✅ **API completa**: Integração fácil com seus plugins  
✅ **Configuração simplificada**: Defaults prontos para uso  

## 💻 Requisitos

### Servidor
- **Java**: 8 ou superior
- **Spigot/Paper**: 1.8.8 até 1.15.2
- **RAM**: Mínimo 2GB (mundos ficam em memória)
- **Maven**: 3.6+ (apenas para compilar)

### Banco de Dados (opcional)
Escolha um dos seguintes:
- **MySQL**: 8.0 ou superior
- **MariaDB**: 10.5 ou superior  
- **PostgreSQL**: 12 ou superior

> **Nota**: Você pode usar armazenamento em arquivo sem banco de dados

## 📦 Instalação

### Passo 1: Compilar o Projeto

```bash
# Clone o repositório
git clone <url-do-repositorio>
cd slimeworldmanager

# Compile com Maven
mvn clean install
```

**Arquivos gerados:**
- `slimeworldmanager-plugin/target/slimeworldmanager-plugin-2.2.0-SNAPSHOT.jar` (Plugin principal)
- `slimeworldmanager-classmodifier/target/slimeworldmanager-classmodifier-2.2.0-SNAPSHOT.jar` (JavaAgent)

### Passo 2: Instalar no Servidor

1. **Copie o plugin** para a pasta `plugins/`:
   ```bash
   slimeworldmanager-plugin-2.2.0-SNAPSHOT.jar → plugins/
   ```

2. **Copie o classmodifier** para a pasta raiz do servidor:
   ```bash
   slimeworldmanager-classmodifier-2.2.0-SNAPSHOT.jar → /
   ```

3. **Modifique o script de inicialização** adicionando o JavaAgent:

   **Linux/Mac (start.sh):**
   ```bash
   #!/bin/bash
   java -Xmx4G -Xms2G \
     -javaagent:slimeworldmanager-classmodifier-2.2.0-SNAPSHOT.jar \
     -jar spigot.jar nogui
   ```

   **Windows (start.bat):**
   ```batch
   @echo off
   java -Xmx4G -Xms2G ^
     -javaagent:slimeworldmanager-classmodifier-2.2.0-SNAPSHOT.jar ^
     -jar spigot.jar nogui
   pause
   ```

4. **Inicie o servidor** para gerar os arquivos de configuração

> ⚠️ **IMPORTANTE**: O JavaAgent é obrigatório! Sem ele o plugin não funcionará.

## ⚙️ Configuração

### 1. Configurar Data Sources (`plugins/SlimeWorldManager/sources.yml`)

O plugin suporta múltiplos tipos de armazenamento simultaneamente:

#### 📁 Armazenamento em Arquivo (Padrão)
```yaml
file:
  path: slime_worlds  # Pasta onde os mundos serão salvos
```

#### 🐬 MySQL
```yaml
mysql:
  enabled: true
  host: localhost
  port: 3306
  username: root
  password: ''
  database: database
  # Configurações de pool (opcional)
  max-pool-size: 10
  min-idle: 2
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

#### 🦭 MariaDB
```yaml
mariadb:
  enabled: true
  host: localhost
  port: 3306
  username: root
  password: ''
  database: database
  # Mesmas configurações de pool do MySQL
```

#### 🐘 PostgreSQL
```yaml
postgresql:
  enabled: true
  host: localhost
  port: 5432
  username: postgres
  password: ''
  database: postgres
  # Mesmas configurações de pool
```

### 2. Configurar Mundos (`plugins/SlimeWorldManager/worlds.yml`)

```yaml
worlds:
  # Mundo de lobby
  lobby:
    source: file              # Data source: mysql, mariadb, postgresql, file
    loadOnStartup: true       # Carregar ao iniciar o servidor
    readOnly: false           # true = não salva alterações
    
    # Propriedades do mundo
    spawn: 0, 100, 0          # Coordenadas de spawn (x, y, z)
    difficulty: normal        # peaceful, easy, normal, hard
    allowMonsters: false
    allowAnimals: true
    pvp: false
    environment: NORMAL       # NORMAL, NETHER, THE_END
    worldType: default        # default, flat, large_biomes, amplified

  # Mundo de minigame (template)
  bedwars-template:
    source: mysql
    loadOnStartup: false      # Não carregar automaticamente
    readOnly: true            # Modo somente leitura (para templates)
    spawn: 0, 64, 0
    difficulty: normal
    allowMonsters: true
    allowAnimals: false
    pvp: true
```

### 3. Preparar o Banco de Dados

#### MySQL / MariaDB
```sql
-- Criar banco de dados
CREATE DATABASE slimeworldmanager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Criar usuário
CREATE USER 'slimeworldmanager'@'%' IDENTIFIED BY 'senha_segura_aqui';

-- Conceder permissões
GRANT ALL PRIVILEGES ON slimeworldmanager.* TO 'slimeworldmanager'@'%';
FLUSH PRIVILEGES;
```

#### PostgreSQL
```sql
-- Criar banco de dados
CREATE DATABASE slimeworldmanager
  WITH ENCODING 'UTF8'
  LC_COLLATE = 'en_US.UTF-8'
  LC_CTYPE = 'en_US.UTF-8';

-- Criar usuário
CREATE USER slimeworldmanager WITH PASSWORD 'senha_segura_aqui';

-- Conceder permissões
GRANT ALL PRIVILEGES ON DATABASE slimeworldmanager TO slimeworldmanager;
```

> **Dica**: As tabelas são criadas automaticamente pelo plugin na primeira execução.

## 🎮 Comandos

Todos os comandos possuem os aliases `/swm`, `/slime` e `/slimeworld`.

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/swm help` | Mostra a ajuda | `swm.help` |
| `/swm list` | Lista mundos carregados | `swm.list` |
| `/swm load <mundo> [source]` | Carrega um mundo | `swm.loadworld` |
| `/swm unload <mundo>` | Descarrega um mundo | `swm.unloadworld` |
| `/swm goto <mundo>` | Teleporta para um mundo | `swm.gotoworld` |
| `/swm import <pasta> <mundo> <source>` | Importa mundo tradicional | `swm.importworld` |
| `/swm migrate <mundo> <de> <para>` | Migra entre data sources | `swm.migrate` |
| `/swm delete <mundo> <source>` | Deleta um mundo | `swm.deleteworld` |

### Exemplos de Uso

```bash
# Carregar um mundo do MySQL
/swm load meu-mundo mysql

# Importar um mundo tradicional para PostgreSQL
/swm import world bedwars-map1 postgresql

# Migrar um mundo de arquivo para MySQL
/swm migrate lobby file mysql

# Teleportar para um mundo
/swm goto lobby
```

## 📚 API para Desenvolvedores

### Adicionar Dependência

**Maven:**
```xml
<repository>
    <id>local-repo</id>
    <url>file://${project.basedir}/libs</url>
</repository>

<dependency>
    <groupId>com.grinderwolf</groupId>
    <artifactId>slimeworldmanager-api</artifactId>
    <version>2.2.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

**Gradle:**
```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    compileOnly 'com.grinderwolf:slimeworldmanager-api:2.2.0-SNAPSHOT'
}
```

### Exemplos de Código

#### Carregar um Mundo
```java
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.*;
import org.bukkit.Bukkit;

public class ExemploCarregarMundo {
    
    public void carregarMundo() {
        // Obter a API
        SlimePlugin slimePlugin = (SlimePlugin) Bukkit.getPluginManager()
            .getPlugin("SlimeWorldManager");
        
        // Obter o loader (mysql, mariadb, postgresql, file)
        SlimeLoader loader = slimePlugin.getLoader("mysql");
        
        // Configurar propriedades do mundo
        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setString(SlimeProperties.DIFFICULTY, "normal");
        properties.setInt(SlimeProperties.SPAWN_X, 0);
        properties.setInt(SlimeProperties.SPAWN_Y, 100);
        properties.setInt(SlimeProperties.SPAWN_Z, 0);
        properties.setBoolean(SlimeProperties.ALLOW_MONSTERS, true);
        properties.setBoolean(SlimeProperties.ALLOW_ANIMALS, true);
        properties.setBoolean(SlimeProperties.PVP, true);
        properties.setString(SlimeProperties.ENVIRONMENT, "NORMAL");
        
        try {
            // Carregar o mundo (assíncrono recomendado)
            SlimeWorld world = slimePlugin.loadWorld(
                loader, 
                "meu-mundo", 
                false,  // readOnly
                properties
            );
            
            // Gerar o mundo no servidor (deve ser síncrono)
            Bukkit.getScheduler().runTask(plugin, () -> {
                slimePlugin.generateWorld(world);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### Clonar Mundo para Minigame
```java
public class ExemploClonarMundo {
    
    public void clonarMundoTemplate(String nomeTemplate, String nomeClone) {
        SlimePlugin slimePlugin = (SlimePlugin) Bukkit.getPluginManager()
            .getPlugin("SlimeWorldManager");
        
        SlimeLoader loader = slimePlugin.getLoader("mysql");
        
        try {
            // Carregar template em modo somente leitura
            SlimeWorld template = slimePlugin.loadWorld(
                loader,
                nomeTemplate,
                true,  // readOnly = true (não salva alterações)
                new SlimePropertyMap()
            );
            
            // Clonar o mundo
            SlimeWorld clone = template.clone(nomeClone);
            
            // Gerar o clone
            slimePlugin.generateWorld(clone);
            
            // Agora você pode usar o mundo clonado
            World bukkitWorld = Bukkit.getWorld(nomeClone);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### Importar Mundo Tradicional
```java
import java.io.File;

public class ExemploImportar {
    
    public void importarMundo() {
        SlimePlugin slimePlugin = (SlimePlugin) Bukkit.getPluginManager()
            .getPlugin("SlimeWorldManager");
        
        SlimeLoader loader = slimePlugin.getLoader("postgresql");
        
        File worldFolder = new File("world");  // Pasta do mundo tradicional
        String worldName = "meu-mundo-importado";
        
        try {
            slimePlugin.importWorld(worldFolder, worldName, loader);
            System.out.println("Mundo importado com sucesso!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### Migrar Entre Data Sources
```java
public class ExemploMigrar {
    
    public void migrarMundo() {
        SlimePlugin slimePlugin = (SlimePlugin) Bukkit.getPluginManager()
            .getPlugin("SlimeWorldManager");
        
        SlimeLoader loaderOrigem = slimePlugin.getLoader("file");
        SlimeLoader loaderDestino = slimePlugin.getLoader("mysql");
        
        String worldName = "lobby";
        
        try {
            slimePlugin.migrateWorld(worldName, loaderOrigem, loaderDestino);
            System.out.println("Mundo migrado de file para mysql!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Propriedades Disponíveis

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `SPAWN_X` | int | 0 | Coordenada X do spawn |
| `SPAWN_Y` | int | 255 | Coordenada Y do spawn |
| `SPAWN_Z` | int | 0 | Coordenada Z do spawn |
| `DIFFICULTY` | String | "normal" | Dificuldade (peaceful, easy, normal, hard) |
| `ALLOW_MONSTERS` | boolean | true | Permitir spawn de monstros |
| `ALLOW_ANIMALS` | boolean | true | Permitir spawn de animais |
| `PVP` | boolean | true | Ativar PvP |
| `ENVIRONMENT` | String | "NORMAL" | Ambiente (NORMAL, NETHER, THE_END) |
| `WORLD_TYPE` | String | "default" | Tipo do mundo |

> 📖 **Documentação completa**: Veja mais exemplos na pasta `.docs/api/`

## 📌 Compatibilidade

| Versão Minecraft | Suporte | Notas |
|------------------|---------|-------|
| 1.8.8 | ✅ Completo | Requer spigot-1.8.8.jar em `libs/` |
| 1.9 - 1.15.2 | ✅ Completo | Suporte nativo |
| 1.16+ | ❌ Não suportado | Requer atualização do NMS |

### Servidores Testados
- ✅ Spigot 1.8.8 - 1.15.2
- ✅ PaperSpigot 1.8.8 - 1.15.2
- ✅ PandaSpigot 1.8.8

### Bancos de Dados Testados
- ✅ MySQL 8.0+
- ✅ MariaDB 10.5+
- ✅ PostgreSQL 12+

## 🏗️ Estrutura do Projeto

```
slimeworldmanager/
├── slimeworldmanager-api/          # API pública
├── slimeworldmanager-plugin/       # Implementação do plugin
├── slimeworldmanager-nms-common/   # Código NMS comum
├── slimeworldmanager-nms-v1_8_R3/  # Implementação NMS 1.8
├── slimeworldmanager-importer/     # Importador de mundos
├── slimeworldmanager-classmodifier/# JavaAgent para modificação de classes
└── .docs/                          # Documentação
```

## ⚠️ Notas Importantes

### Requisitos Obrigatórios
- ✅ **JavaAgent é obrigatório**: O classmodifier deve estar no comando de inicialização
- ✅ **RAM suficiente**: Mundos SRF ficam em memória durante o uso
- ✅ **Backup regular**: Sempre faça backup dos seus mundos

### Limitações Conhecidas
- ❌ **Não compatível com Multiverse-Core**: Use apenas para mundos SRF
- ❌ **Mundos grandes**: Considere o uso de RAM (1 mundo ≈ 50-200MB)
- ⚠️ **Multi-servidor**: Configure locks corretamente para evitar corrupção

### Boas Práticas
1. **Use readOnly para templates**: Mundos de minigame devem ser somente leitura
2. **Clone ao invés de carregar**: Para minigames, clone o template para cada partida
3. **Descarregue mundos não usados**: Libere RAM descarregando mundos inativos
4. **Monitore conexões do banco**: Ajuste `max-pool-size` conforme necessário
5. **Teste antes de produção**: Sempre teste em ambiente de desenvolvimento

### Troubleshooting

**Erro: "SlimeWorldManager does not support Spigot vX_X_RX"**
- Solução: Versão do Minecraft não suportada ou JavaAgent não carregado

**Erro: "World is locked"**
- Solução: Outro servidor está usando o mundo ou houve crash anterior
- Execute: `/swm unlock <mundo> <source>` (se tiver certeza que não está em uso)

**Erro: "Could not connect to database"**
- Solução: Verifique credenciais, host e porta no `sources.yml`
- Teste conexão: `telnet <host> <port>`

**Mundo não carrega/fica em branco**
- Solução: Verifique se o mundo existe no data source
- Liste mundos: `/swm list`

## 🤝 Contribuindo

Contribuições são muito bem-vindas! Aqui está como você pode ajudar:

### Reportar Bugs
1. Verifique se o bug já não foi reportado
2. Crie uma issue detalhada com:
   - Versão do Minecraft/Spigot
   - Versão do plugin
   - Logs de erro completos
   - Passos para reproduzir

### Sugerir Features
- Abra uma issue descrevendo a feature
- Explique o caso de uso
- Discuta a implementação

### Enviar Pull Requests
1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/MinhaFeature`
3. Commit suas mudanças: `git commit -m 'Adiciona MinhaFeature'`
4. Push para a branch: `git push origin feature/MinhaFeature`
5. Abra um Pull Request

### Melhorar Documentação
- Corrija erros de digitação
- Adicione exemplos
- Traduza para outros idiomas
- Melhore explicações

## 👨‍💻 Autor

**Oharium** (Haridade)

Versão modificada com suporte multi-database baseada no SlimeWorldManager original.

## 📜 Licença

Este projeto mantém a licença MIT do SlimeWorldManager original.

## � Créditos

- **Grinderwolf**: Autor original do SlimeWorldManager
- **Hypixel Team**: Criadores do Slime Region Format
- **Comunidade Spigot**: Suporte e feedback

## 🔗 Links Úteis

### Ferramentas
- [MySQL Workbench](https://dev.mysql.com/downloads/workbench/) - Cliente MySQL
- [pgAdmin](https://www.pgadmin.org/download/) - Cliente PostgreSQL
- [DBeaver](https://dbeaver.io/) - Cliente universal de banco de dados
- [HeidiSQL](https://www.heidisql.com/) - Cliente MySQL/MariaDB

### Downloads
- [MySQL](https://dev.mysql.com/downloads/mysql/)
- [MariaDB](https://mariadb.org/download/)
- [PostgreSQL](https://www.postgresql.org/download/)
- [Spigot BuildTools](https://www.spigotmc.org/wiki/buildtools/)

### Documentação
- [Spigot API](https://hub.spigotmc.org/javadocs/spigot/)
- [Paper API](https://papermc.io/javadocs/)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)

---

<div align="center">

**⭐ Se este projeto foi útil, considere dar uma estrela!**

Desenvolvido com ❤️ para a comunidade Minecraft

</div>
