execute store result score @s count run data get storage minecraft:rider1 pickcount
scoreboard players remove @s count 64
execute store result storage minecraft:rider1 pickcount int 1 run scoreboard players get @s count
function imuzen127x74:collect/3c with storage minecraft:rider1


data modify storage minecraft:rider1 pickcount set value 64
function imuzen127x74:collect/3d with storage minecraft:rider1



