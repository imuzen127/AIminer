execute as @e[tag=rider1] at @s at @n[tag=test1] run tp @s ~ ~ ~
execute as @e[tag=rider1] at @s rotated as @n[tag=test1] run rotate @s ~ ~
team join daburu @e[tag=two]
execute as @e[tag=test1] run data modify entity @s AngryAt set from entity @e[limit=1,tag=aim1] UUID
execute as @e[tag=test1] run data modify entity @s AngerTime set value 1

execute as @e[tag=test1] store result score @s movex run data get entity @s Motion[0] 1000
execute as @e[tag=test1] store result score @s movez run data get entity @s Motion[2] 1000

execute as @e[tag=aim1] store result score @s aimx run data get entity @s Pos[0]
execute as @e[tag=aim1] store result storage minecraft:rider1 aimx int 1 run scoreboard players get @s aimx
execute as @e[tag=aim1] store result score @s aimy run data get entity @s Pos[1]
execute as @e[tag=aim1] store result storage minecraft:rider1 aimy int 1 run scoreboard players get @s aimy
execute as @e[tag=aim1] store result score @s aimz run data get entity @s Pos[2]
execute as @e[tag=aim1] store result storage minecraft:rider1 aimz int 1 run scoreboard players get @s aimz



execute as @e[tag=rider1] store result storage minecraft:rider1 distance double 1 run attribute @s block_interaction_range get

execute as @e[tag=test1] at @s run function imuzen127x74:ifmine

execute as @e[tag=test1] at @s if entity @e[type=item,distance=..1] run function imuzen127x74:collect/1

