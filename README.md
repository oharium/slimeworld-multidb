# SlimeWorldManager - PostgreSQL Edition

Uma versão aprimorada do SlimeWorldManager com suporte nativo a PostgreSQL, desenvolvida para servidores que precisam de maior escalabilidade e confiabilidade no armazenamento de mundos.

## 🚀 Sobre o Projeto

O SlimeWorldManager implementa o Slime Region Format (SRF), um formato de armazenamento de mundos desenvolvido pela equipe do Hypixel para carregar e salvar mundos de forma mais eficiente.

Esta versão adiciona:

✅ Suporte completo a MySQL, MariaDB e PostgreSQL  
✅ Pool de conexões otimizado com HikariCP  
✅ Melhor escalabilidade para redes grandes  
✅ Sistema de locks para prevenir corrupção de dados  
✅ Ideal para servidores com múltiplas instâncias (BedWars, Duels, SkyWars, etc.)  
✅ Carregamento assíncrono de mundos  
✅ API completa para desenvolvedores  

## 💻 Tecnologias Utilizadas

- Java 8+
- MySQL 8.0+ / MariaDB 10.5+ / PostgreSQL 12+
- Maven 3.6+
- Spigot/Paper 1.8.8 - 1.15.2
- HikariCP (Connection Pooling)
- Lombok
- Flow-NBT

## 📦 Instalação

### 1. Build do Projeto

```bash
mvn clean package
```

Os arquivos gerados estarão em:
- Plugin: `slimeworldmanager-plugin/target/slimeworldmanager-plugin-2.2.0-SNAPSHOT.jar`
- ClassModifier: `slimeworldmanager-classmodifier/target/slimeworldmanager-classmodifier-2.2.0-SNAPSHOT.jar`

### 2. Instalação no Servidor

1. Coloque o `slimeworldmanager-plugin-<version>.jar` na pasta `plugins/`
2. Coloque o `slimeworldmanager-classmodifier-<version>.jar` na pasta raiz do servidor
3. Modifique o comando de inicialização do servidor adicionando antes do `-jar`:

```bash
-javaagent:slimeworldmanager-classmodifier-<version>.jar
```

Exemplo completo:
```bash
java -Xmx2G -javaagent:slimeworldmanager-classmodifier-2.2.0-SNAPSHOT.jar -jar spigot.jar
```

## ⚙️ Configuração

### PostgreSQL (sources.yml)

```yaml
postgresql:
  enabled: true
  host: 127.0.0.1
  port: 5432
  username: slimeworldmanager
  password: sua_senha_aqui
  database: slimeworldmanager

file:
  path: slime_worlds
```

### Configurando Mundos (worlds.yml)

### Configurando Mundos (worlds.yml)

```yaml
worlds:
  meu-mundo:
    source: mysql  # ou mariadb, postgresql, file
    spawn: 0, 100, 0
    difficulty: normal
    allowMonsters: true
    allowAnimals: true
```

### Criando o Banco de Dados

**MySQL/MariaDB:**
```sql
CREATE DATABASE slimeworldmanager;
CREATE USER 'slimeworldmanager'@'%' IDENTIFIED BY 'sua_senha';
GRANT ALL PRIVILEGES ON slimeworldmanager.* TO 'slimeworldmanager'@'%';
FLUSH PRIVILEGES;
```

**PostgreSQL:**
```sql
CREATE DATABASE slimeworldmanager;
CREATE USER slimeworldmanager WITH PASSWORD 'sua_senha';
GRANT ALL PRIVILEGES ON DATABASE slimeworldmanager TO slimeworldmanager;
```

## 🎮 Comandos

- `/swm load <mundo>` - Carrega um mundo
- `/swm unload <mundo>` - Descarrega um mundo
- `/swm goto <mundo>` - Teleporta para um mundo
- `/swm import <mundo> <source>` - Importa um mundo tradicional para SRF
- `/swm migrate <mundo> <source-origem> <source-destino>` - Migra um mundo entre data sources
- `/swm list` - Lista todos os mundos carregados

## 📚 API para Desenvolvedores

```java
// Obter a API
SlimePlugin plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");

// Carregar um mundo do MySQL
SlimeLoader loader = plugin.getLoader("mysql"); // ou "mariadb", "postgresql", "file"
SlimePropertyMap properties = new SlimePropertyMap();
properties.setString(SlimeProperties.DIFFICULTY, "normal");
properties.setInt(SlimeProperties.SPAWN_X, 0);
properties.setInt(SlimeProperties.SPAWN_Y, 100);
properties.setInt(SlimeProperties.SPAWN_Z, 0);

// Carregar assincronamente
SlimeWorld world = plugin.loadWorld(loader, "meu-mundo", properties);

// Gerar o mundo (síncrono)
plugin.generateWorld(world);
```

Veja mais exemplos na pasta `.docs/api/`

## 📌 Compatibilidade

| Versão | Suporte |
|--------|---------|
| 1.8.8 - 1.15.2 | ✅ Completo |
| 1.16+ | ❌ Não suportado |

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

- Este projeto é um fork do SlimeWorldManager original
- Requer o uso do JavaAgent (classmodifier) para funcionar corretamente
- Mundos SRF são mantidos em memória, considere o uso de RAM
- Não é compatível com Multiverse-Core para gerenciamento de mundos SRF

## 🤝 Contribuições

Contribuições são bem-vindas! Sinta-se à vontade para:

- Reportar bugs
- Sugerir novas features
- Enviar Pull Requests
- Melhorar a documentação

## 👨‍💻 Desenvolvedor

**Oharium** (Haridade)

## 📜 Licença

Este projeto mantém a licença do projeto original SlimeWorldManager.

## 🔗 Links Úteis

- [Documentação Original](https://www.spigotmc.org/resources/slimeworldmanager.69974/)
- [MySQL Download](https://dev.mysql.com/downloads/mysql/)
- [MariaDB Download](https://mariadb.org/download/)
- [PostgreSQL Download](https://www.postgresql.org/download/)
- [PgAdmin](https://www.pgadmin.org/download/)
- [phpMyAdmin](https://www.phpmyadmin.net/)

---

**Projeto baseado no SlimeWorldManager original por Grinderwolf**
