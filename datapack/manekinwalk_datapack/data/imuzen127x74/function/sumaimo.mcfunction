kill @e[tag=aim1o]
kill @e[tag=aim1s]
summon armor_stand ~ ~ ~ {Invisible:1b,Invulnerable:1b,Tags:["aim1o"],NoGravity:1b}
execute as @e[tag=rider1] at @s unless entity @e[tag=aim1o,distance=..4.5] run scoreboard players set @s minepoint 0

