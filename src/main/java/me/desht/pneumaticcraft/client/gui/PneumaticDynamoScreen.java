/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.api.crafting.TemperatureRange;
import me.desht.pneumaticcraft.client.gui.widget.WidgetAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetEnergy;
import me.desht.pneumaticcraft.client.gui.widget.WidgetTemperature;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.client.util.PointXY;
import me.desht.pneumaticcraft.common.block.entity.PneumaticDynamoBlockEntity;
import me.desht.pneumaticcraft.common.inventory.PneumaticDynamoMenu;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.energy.CapabilityEnergy;

import java.util.ArrayList;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class PneumaticDynamoScreen extends AbstractPneumaticCraftContainerScreen<PneumaticDynamoMenu,PneumaticDynamoBlockEntity> {
    private WidgetAnimatedStat inputStat;
    private WidgetTemperature tempWidget;

    public PneumaticDynamoScreen(PneumaticDynamoMenu container, Inventory inv, Component displayString) {
        super(container, inv, displayString);
    }

    @Override
    public void init() {
        super.init();
        inputStat = addAnimatedStat(new TextComponent("Output"), Textures.GUI_BUILDCRAFT_ENERGY, 0xFF555555, false);

        te.getCapability(CapabilityEnergy.ENERGY).ifPresent(storage -> addRenderableWidget(new WidgetEnergy(leftPos + 20, topPos + 20, storage)));
        addRenderableWidget(tempWidget = new WidgetTemperature(leftPos + 97, topPos + 20, TemperatureRange.of(273, 673), 273, 50)
                .setOperatingRange(TemperatureRange.of(323, 625)).setShowOperatingRange(false));
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return Textures.GUI_4UPGRADE_SLOTS;
    }

    @Override
    public void containerTick() {
        super.containerTick();

        inputStat.setText(getOutputStat());
        tempWidget.setTemperature(te.getHeatExchanger().getTemperatureAsInt());
        tempWidget.autoScaleForTemperature();
    }

    private List<Component> getOutputStat() {
        List<Component> textList = new ArrayList<>();
        textList.add(xlate("pneumaticcraft.gui.tab.status.pneumaticDynamo.maxEnergyProduction").withStyle(ChatFormatting.GRAY));
        textList.add(new TextComponent(te.getRFRate() + " FE/t").withStyle(ChatFormatting.BLACK));
        textList.add(xlate("pneumaticcraft.gui.tab.status.pneumaticDynamo.maxOutputRate").withStyle(ChatFormatting.GRAY));
        textList.add(new TextComponent(te.getRFRate() * 2 + " FE/t").withStyle(ChatFormatting.BLACK));
        textList.add(xlate("pneumaticcraft.gui.tab.status.fluxCompressor.storedEnergy").withStyle(ChatFormatting.GRAY));
        textList.add(new TextComponent(te.getInfoEnergyStored() + " FE").withStyle(ChatFormatting.BLACK));
        return textList;
    }

    @Override
    protected void addPressureStatInfo(List<Component> pressureStatText) {
        super.addPressureStatInfo(pressureStatText);
        pressureStatText.add(xlate("pneumaticcraft.gui.tooltip.maxUsage", te.getAirRate()).withStyle(ChatFormatting.BLACK));
    }

    @Override
    public void addProblems(List<Component> curInfo) {
        super.addProblems(curInfo);
        if (te.getEfficiency() < 100) {
            curInfo.addAll(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.problems.advancedAirCompressor.efficiency", te.getEfficiency() + "%"));
        }
    }

    @Override
    protected PointXY getGaugeLocation() {
        return super.getGaugeLocation().add(10, 0);
    }
}
