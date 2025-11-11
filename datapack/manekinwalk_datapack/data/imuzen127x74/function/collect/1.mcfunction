

data modify storage minecraft:rider1 pickid set from entity @n[type=item] Item.id
data modify storage minecraft:rider1 pickcount set from entity @n[type=item] Item.count
data modify storage minecraft:rider1 pickcomponents set from entity @n[type=item] Item.components
data modify storage minecraft:rider1 Inventory set from entity @s data.Inventory
execute unless data entity @s data.Inventory run data modify storage minecraft:rider1 Inventory set value []

function imuzen127x74:collect/2 with storage minecraft:rider1

execute if score @s pickup matches 1 run function imuzen127x74:collect/3
execute if score @s pickup matches 0 run function imuzen127x74:collect/4

#execute if score @s count matches 64 run function imuzen127x74:collect/3c with storage minecraft:rider1

function imuzen127x74:collect/5
execute if score @s pickup matches 1 run function imuzen127x74:collect/6
execute if score @s pickup matches 0 run function imuzen127x74:collect/7

data modify storage minecraft:rider1 pickid set value ""
data modify storage minecraft:rider1 pickcount set value ""
data modify storage minecraft:rider1 pickcomponents set value ""
data modify storage minecraft:rider1 Inventory set value ""
