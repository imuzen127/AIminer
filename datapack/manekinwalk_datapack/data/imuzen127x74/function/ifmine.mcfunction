execute unless score @s movex matches 0 run scoreboard players set @s minepoint 0
execute unless score @s movez matches 0 run scoreboard players set @s minepoint 0
execute unless entity @e[distance=..4.5,tag=aim1o] unless entity @e[distance=..4.5,tag=aim1s] run scoreboard players set @s minepoint 0


execute if score @s movex matches 0 if score @s movez matches 0 if entity @e[distance=..4.5,tag=aim1o] run scoreboard players add @s minepoint 1
execute if score @s movex matches 0 if score @s movez matches 0 if entity @e[distance=..4.5,tag=aim1s] run scoreboard players add @s minepoint 1

