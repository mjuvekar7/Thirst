/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.thirst;

import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.CharacterMoveInputEvent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.registry.In;
import org.terasology.thirst.component.DrinkComponent;
import org.terasology.thirst.component.ThirstComponent;
import org.terasology.thirst.event.DrinkConsumedEvent;

/**
 * This authority system handles drink consumption by various entities.
 */
@RegisterSystem(value = RegisterMode.AUTHORITY)
public class ThirstAuthoritySystem extends BaseComponentSystem {
    @In
    private EntityManager entityManager;
    @In
    private Time time;

    /**
     * Initialize thirst attributes for a spawned player. Called when a player is spawned.
     *
     * @param event  the event corresponding to the spawning of the player
     * @param player a reference to the player entity
     * @param thirst a thirst component to assign to the player
     */
    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player,
                              ThirstComponent thirst) {
        thirst.lastCalculatedWater = thirst.maxWaterCapacity;
        thirst.lastCalculationTime = time.getGameTimeInMs();
        player.saveComponent(thirst);
    }

    /**
     * Defines what to do when an entity is removed.
     *
     * @param event  the event corresponding to the removal of an entity
     * @param entity the entity being removed
     * @param thirst the thirst component associated with the entity
     */
    @ReceiveEvent
    public void beforeRemoval(BeforeDeactivateComponent event, EntityRef entity, ThirstComponent thirst) {
        thirst.lastCalculatedWater = ThirstUtils.getThirstForEntity(entity);
        thirst.lastCalculationTime = time.getGameTimeInMs();
        entity.saveComponent(thirst);
    }

    /**
     * Defines what happens when a drink is consumed.
     *
     * @param event the event corresponding to the consumption of a drink
     * @param item  the item that the player is drinking
     * @param drink the drink component associated with the item being consumed
     */
    @ReceiveEvent
    public void drinkConsumed(ActivateEvent event, EntityRef item, DrinkComponent drink) {
        float filling = drink.filling;
        EntityRef instigator = event.getInstigator();
        ThirstComponent thirst = instigator.getComponent(ThirstComponent.class);
        if (thirst != null) {
            thirst.lastCalculatedWater = Math.min(thirst.maxWaterCapacity, ThirstUtils.getThirstForEntity(instigator) + filling);
            thirst.lastCalculationTime = time.getGameTimeInMs();
            instigator.saveComponent(thirst);
            item.send(new DrinkConsumedEvent());
            event.consume();
        }
    }

    /**
     * Updates the thirst attribute of the character upon movement.
     *
     * @param event     the event associated with the movement of the character
     * @param character the character that has moved
     * @param thirst    the thirst component associated with the character
     */
    @ReceiveEvent
    public void characterMoved(CharacterMoveInputEvent event, EntityRef character, ThirstComponent thirst) {
        final float expectedDecay = event.isRunning() ? thirst.sprintDecayPerSecond : thirst.normalDecayPerSecond;
        if (expectedDecay != thirst.waterDecayPerSecond) {
            // Recalculate current thirst and apply new decay
            thirst.lastCalculatedWater = ThirstUtils.getThirstForEntity(character);
            thirst.lastCalculationTime = time.getGameTimeInMs();
            thirst.waterDecayPerSecond = expectedDecay;
            character.saveComponent(thirst);
        }
    }
}
