kill @e[tag=aim1]
summon armor_stand ~ ~ ~ {Invisible:1b,Invulnerable:1b,Tags:["aim1"],NoGravity:1b}
execute as @e[tag=rider1] at @s unless entity @e[tag=aim1,distance=..4.5] run scoreboard players set @s minepoint 0

