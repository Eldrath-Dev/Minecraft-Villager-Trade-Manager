# Minecraft Villager Trade Manager

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x--1.21.x-green.svg)](https://minecraft.net)
[![Paper](https://img.shields.io/badge/Paper-1.20.x--1.21.x-blue.svg)](https://papermc.io)
[![Spigot](https://img.shields.io/badge/Spigot-1.20.x--1.21.x-orange.svg)](https://spigotmc.org)
[![Java](https://img.shields.io/badge/Java-17%2B-red.svg)](https://adoptium.net)

**Complete control over villager trading economics with custom pricing and discount prevention**

Minecraft Villager Trade Manager is a comprehensive Paper/Spigot plugin that gives server administrators complete control over villager trading economics. Take control of trade prices, eliminate exploitative discounts, and create a fair, balanced trading experience for all players.

## 🎯 Features

### Complete Trade Control
- Set custom prices for any villager trade, especially enchanted books
- Lock in fixed prices that never change regardless of player reputation
- Prevent all forms of trade manipulation and exploitation

### Discount Prevention System
- Eliminate Hero of the Village effects and free gifts
- Remove reputation-based trade discounts
- Prevent price inflation from overused trades
- Maintain static pricing across all trading scenarios

### Custom Pricing Engine
- Set specific emerald costs for enchanted books (Fortune, Efficiency, Protection, etc.)
- Configure prices for any enchantment level (Fortune I, Fortune II, Fortune III)
- Automatic price capping at 64 emeralds per trade
- Consistent 1-book cost for all enchanted book trades

### Universal Compatibility
- Works with Minecraft versions 1.20.x through 1.21.x
- Compatible with Paper and Spigot server implementations
- Automatic adaptation to server capabilities
- No conflicts with other plugins

### User-Friendly Management
- Simple command system: `/villagertrade setprice <enchant> <level> <price>`
- Intelligent tab completion for all enchantments and levels
- Real-time status monitoring and price management
- Persistent storage with automatic data saving

## 🚀 Installation

1. Download the latest release from the [Releases](https://github.com/alan/Minecraft-Villager-Trade-Manager/releases) page
2. Place the `.jar` file in your server's `plugins` folder
3. Restart your server
4. The plugin will automatically create its configuration files

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/villagertrade on` | Enable trade management system | `villagertrade.manage` |
| `/villagertrade off` | Disable trade management system | `villagertrade.manage` |
| `/villagertrade status` | View current custom prices and system status | `villagertrade.manage` |
| `/villagertrade setprice <enchant> <level> <price>` | Set custom prices for trades | `villagertrade.manage` |

### Command Examples
```bash
# Set Efficiency I to cost 20 emeralds
/villagertrade setprice efficiency 1 20

# Set Protection IV to cost 35 emeralds
/villagertrade setprice protection 4 35

# Set Mending to cost 50 emeralds
/villagertrade setprice mending 1 50

# Set Fortune III to cost 40 emeralds
/villagertrade setprice fortune 3 40

🔄 Compatibility

Minecraft Versions: 1.20.x - 1.21.x (Automatically adapts to server capabilities)

Server Platforms: Paper, Spigot

Java Version: Java 17+

No Dependencies: Works out-of-the-box with no additional requirements

📊 Benefits

Economic Balance: Create fair trade prices that work for your server economy

Exploit Prevention: Stop players from abusing villager discount mechanics

Admin Control: Complete authority over villager trading economics

Player Satisfaction: Consistent, predictable trading for all players

Server Stability: Eliminate trading-related economic imbalances

🛠️ Technical Features

Cross-Version Compatibility: Works with multiple Minecraft versions

Reflection-Based Compatibility: Handles version-specific API differences

Thread-Safe Operations: Concurrent data handling with ConcurrentHashMap

Resource Management: Proper cleanup and memory management

Graceful Error Handling: Helpful error messages and recovery systems



📄 License
This project is licensed under the MIT License - see the LICENSE file for details.

🙏 Acknowledgments
Thanks to the Bukkit/Spigot/Paper development teams
Inspired by the need for better villager trading control on Minecraft servers