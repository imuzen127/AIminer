scoreboard players set @s pickup 0
$execute if data storage minecraft:rider1 Inventory.[{id:"$(pickid)"}] run scoreboard players set @s pickup 1
$execute if data storage minecraft:rider1 Inventory.[{id:"$(pickid)"}] run data modify storage minecraft:rider1 nowcount set from storage minecraft:rider1 Inventory.[{id:"$(pickid)"}].count

