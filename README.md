# Features:
A pair of constructed telepads will allow you to instantly teleport between them.\
Starting with version 3.0, you can now upgrade telepads!\
The upgraded HyperPads can transport ALL entities within a specific range of the player to the destination HyperPad.

HyperPad and Telepad Example coming soon!

# Usage:
## To construct a set of telepads:

  1. Dig a hole 1 block deep and place a sign in the hole.
  2. On top of that sign place a block specified by telepad_center.
  3. Place a stone pressure plate on top of the center block.
  4. On each side of the center block, place a block specified by telepad_surrounding.
  5. Once both telepads are constructed and with redstone dust in hand, right click one pressure plate and then left click the other.

## To Upgrade to HyperPads:

  1. Add 4 blocks of telepad_hyper block to the existing telepads
  2. Step onto the HyperPad to begin wide-area Entity Transport

## To delink a telepad:

  1. Break the stone pressure plate
  2. Left click the center block with redstone

# Commands:
None at this time

# Permissions:
None at this time

## Configuration:

  * max_telepad_distance: Maximum distance over which a telepad pair will operate. to disable set to 0.
  * telepad_center: The ID for the center block of the telepad. Default block is Lapis.
  * enable_surrounding_blocks: Disallow single block telepads. Default is true.
  * telepad_surrounding: The ID for the blocks surrounding the center. Default block is Obsidian.
  * enable_hyper_blocks: If set to true, wide-area entity transportation will be enabled. Requires enable_surrounding_blocks at the moment.
  * telepad_hyper:The ID for the hyper blocks. Default block is Sea Lantern.
  * xRange, yRange, zRange: This integer value determines the range around the telepad entities can be teleported from. Default is 2 for each.
  * op_only: If set to true, only OP may construct telepads. Default is false.
  * disable_teleport_wait: If set to true, telepads activate instantly. Default is false.
  * send_wait_timer: Time in seconds to wait before teleport occurs. Default is 3.
  * disable_teleport_message: If set to true, telepads will not notify you of activation. Default is false.

## Installation
Place telepads.jar in your plugins folder and reload the server.




