# Lumen – Advanced Lighting for Minecraft

**Lumen** is a **Minecraft** plugin designed to enhance server quality of life by optimizing lighting. It allows efficient area illumination using commands and offers unique items:

- **Lumen Torch** – Automatically lights up the surroundings with custom light level.
- **Lumen Guard** – Prevents hostile mobs from spawning.

![Discord](https://img.shields.io/discord/1079917552588816484?label=Discord&logo=discord&logoColor=white&color=31FFA3&style=for-the-badge) ![](https://img.shields.io/badge/Made%20with-%E2%9D%A4%EF%B8%8F%20by%20stargaze-31FFA3?style=for-the-badge) [![CodeFactor](https://img.shields.io/codefactor/grade/github/soystargaze/lumen?style=for-the-badge&logo=codefactor&logoColor=white&color=31FFA3)](https://www.codefactor.io/repository/github/soystargaze/lumen)

![Banner Logo](https://cdn.modrinth.com/data/5WB5vvtt/images/ed1c78a69e6aba737ccc687acc242140fcce6299.png)

---

## ⚡️ Features

- **Dynamic lighting** using **commands** and **custom crafteable items** (No resource pack needed).
- **Auto-lighting** torches with custom light level (Lumen Torch).
- Mob spawn prevention (Lumen Guard).
- **Integration** with **CoreProtect & FAWE**. (Recommended)
- **Multilingual Support** – Available translations: `es_es.yml`, `zh_cn.yml`, `en_en.yml`, `fr_fr.yml`, `de_de.yml`, `it_it.yml`, `pt_br.yml`. Custom languages can be added.

---

## 🛠️ Compatible Integrations

Lumen can leverage other tools to enhance performance and functionality:

- **CoreProtect** – Logs light placements and removals, allowing rollbacks and audits.
- **FastAsyncWorldEdit (FAWE)** – Optimizes light block placement through commands and the Lumen Torch, improving server performance.

  ![Lumen Torch with FAWE](https://imgur.com/pgmWWE1.gif)
  ![LightCommandWithFAWE](https://imgur.com/tqJ3gLA.gif)

These integrations are optional but recommended for better control and efficiency.

---

## 📌 Installation Guide

<details><summary>Installation</summary>

## **Prerequisites**
Before installing Lumen, make sure your server meets the following requirements:

- **Minecraft Server:** PaperMC **1.21 or higher** (recommended **1.21.4**, the latest stable version).
- **Java:** Version **21 or higher**.
- **Optional Dependencies:**
    - **CoreProtect (Optional):** Enables tracking and rollback of placed or removed lights. Integration can be verified in the console upon server startup.
    - **FastAsyncWorldEdit (Optional):** Optimizes performance for placing and removing large amounts of lights.

---

## **Step 1: Download the Plugin**
Download the latest version of **Lumen** from [Modrinth](https://modrinth.com/plugin/lumen) and ensure you obtain a valid `.jar` file.

---

## **Step 2: Installation**
1. **Upload the file** `Lumen.jar` to the `plugins/` folder of your PaperMC server.
2. **Restart the server** to automatically generate the configuration files.
3. **Verify installation** by checking the console. If the installation was successful, you will see a message indicating that the plugin has been loaded correctly.

---

## **Step 3: Initial Configuration**
1. **Navigate to the configuration folder:** `plugins/Lumen/`
2. **Edit `config.yml`** to adjust performance settings, such as:
    - `command_lights_per_tick`: Number of lights added per tick when using commands.
    - `torch_lights_per_tick`: Number of lights added per tick when using torches.
    - `torch_tick_interval`: Interval between torch ticks.
    - `mob_torch_radius`: Protection radius of the anti-mob torch.
3. **If using CoreProtect,** check the server console on startup. If integration is successful, you will see a message indicating that CoreProtect has been detected and is active in Lumen.
4. **If using FastAsyncWorldEdit,** ensure it is installed and properly configured to optimize the placement and removal of lights.

---

## **Step 4: Troubleshooting**
- **The plugin does not load:** It is recommended to use **PaperMC 1.21.4**, the latest stable version. Also, ensure you are using Java 21 or higher.
- **CoreProtect errors:** Check the console when starting the server. If integration does not activate, ensure CoreProtect is correctly installed.
- **Low performance when placing lights:** Adjust values in `config.yml` and/or install **FastAsyncWorldEdit** to optimize large-scale block processing.
- Only newly placed `Lumen Torch` and `Lumen Guard` will have effects of changes in config.yml. Previously placed torches will not be affected unless removed and placed again.

---

## **Support & Contact**
If you encounter issues or have questions, contact support on **[Discord](https://erosmari.com/discord)** or refer to the official plugin documentation.

</details>

---

## 🔐 Commands & Permissions

<details>
<summary>Commands</summary>

Lumen also provides a variety of aliases for each command `/lumen`, `/lu`, and `/l`.

- `/lumen light <range> <light_level> <include_skylight>` – Places lights dynamically.
- `/lumen undo` – Undoes previous light placements.
- `/lumen redo` – Redoes removed lights.
- `/lumen remove area <range>` – Removes lights in a specified area.
- `/lumen clear confirm` – Clears all registered lights.
- `/lumen give <player/all> <torch_type> <quantity>` – Gives torches to players.
- `/lumen reload` – Reloads configuration and translations.
- `/lumen lang <language>` – Changes the plugin language.

</details>

<details>
<summary>Permissions</summary>

- `lumen.light` – Permission to use `/lumen light`.
- `lumen.cancel` – Permission to cancel active tasks.
- `lumen.undo` – Permission to undo placements.
- `lumen.redo` – Permission to redo removed lights.
- `lumen.remove` – Permission to remove lights.
- `lumen.clear` – Permission to clear all lights.
- `lumen.give` – Permission to give `Lumen Torch` and `Lumen Guard`.
- `lumen.reload` – Permission to reload configuration and translations.
- `lumen.lang` – Permission to change the language.
- `lumen.craft.torch` – Permission to craft the `Lumen Torch`.
- `lumen.craft.guard` – Permission to craft the `Lumen Guard`.

</details>

---

## Usage

### For Players
- The `Lumen Torch` automatically lights up nearby areas.
    - Players can right-click the air while holding a Lumen Torch and input a light level between 0 and 15.
    - The selected light level will be stored in the torch and applied when placed.
- The `Lumen Guard` prevents mob spawning within its range.
- Torches can be removed without being lost, and their effects disappear when removed.
- To craft them, you need `lumen.craft.torch` and `lumen.craft.guard` permissions.

**Crafting Recipes**
<details>
<summary>Lumen Torch</summary>

![Lumen Torch Recipe](https://cdn.modrinth.com/data/5WB5vvtt/images/3cf389c35844ac90b2f07e8f7194913937712305.png)

</details>
<details>
<summary>Lumen Guard</summary>

![Lumen Guard Recipe](https://cdn.modrinth.com/data/5WB5vvtt/images/64419e0fbf155c4c1aad408f77c3083b2764da6a.png)

</details>

![Lumen Torch](https://imgur.com/3OQtjzg.gif)

---

### For Administrators
- Advanced light management using commands.
- Safe light removal with `/lumen remove` and `/lumen clear confirm`.
- Item distribution using `/lumen give`.
- Full customization through `config.yml` and translation files in `Translations/`.

---

## 💬 Support & Contact

If you have any questions or encounter issues, feel free to contact us on [Discord](https://erosmari.com/discord)

![bstats](https://bstats.org/signatures/bukkit/Lumen%20-%20Ultimate%20Lightning.svg)
