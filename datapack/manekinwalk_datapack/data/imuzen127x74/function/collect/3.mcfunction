scoreboard players set @s pickupo 0
execute store result score @s count run data get storage minecraft:rider1 pickcount
execute store result score @s countn run data get storage minecraft:rider1 nowcount
scoreboard players operation @s count += @s countn

#execute if score @s count matches ..63 run scoreboard players set @s pickupo 1

#execute if score @s pickupo matches 1 run 
function imuzen127x74:collect/3a with storage minecraft:rider1
#execute if score @s pickupo matches 0 run function imuzen127x74:collect/3b

