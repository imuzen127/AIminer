$execute positioned $(x) $(y) $(z) run function imuzen127x74:sumaimo


execute as @e[tag=test1] at @s if entity @e[tag=aim1o,distance=..4.5] if score @s minepoint matches 60.. run scoreboard players set @s miningcheck 1
$execute as @e[tag=test1,limit=1] if score @s miningcheck matches 1 positioned $(x) $(y) $(z) if block ~ ~ ~ minecraft:oak_log run function imuzen127x74:loot_oak

