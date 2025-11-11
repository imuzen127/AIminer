execute store result storage minecraft:rider1 pickcount int 1 run scoreboard players get @s count
$data modify storage minecraft:rider1 Inventory.[{id:"$(pickid)"}].count set from storage minecraft:rider1 pickcount

