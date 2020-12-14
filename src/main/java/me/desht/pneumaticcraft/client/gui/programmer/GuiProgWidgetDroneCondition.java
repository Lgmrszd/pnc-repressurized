package me.desht.pneumaticcraft.client.gui.programmer;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.client.gui.GuiProgrammer;
import me.desht.pneumaticcraft.client.gui.widget.*;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.progwidgets.*;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public abstract class GuiProgWidgetDroneCondition<T extends ProgWidgetDroneCondition> extends GuiProgWidgetOptionBase<T> {
    private WidgetTextFieldNumber textField;

    GuiProgWidgetDroneCondition(T progWidget, GuiProgrammer guiProgrammer) {
        super(progWidget, guiProgrammer);
    }

    @Override
    public void init() {
        super.init();

        if (isSidedWidget()) {
            for (Direction dir : Direction.VALUES) {
                ITextComponent sideName = ClientUtils.translateDirectionComponent(dir);
                WidgetCheckBox checkBox = new WidgetCheckBox(guiLeft + 8, guiTop + 30 + dir.getIndex() * 12, 0xFF404040, sideName,
                        b -> ((ISidedWidget) progWidget).getSides()[dir.getIndex()] = b.checked);
                checkBox.checked = ((ISidedWidget) progWidget).getSides()[dir.getIndex()];
                addButton(checkBox);
            }
        }

        int baseX = isSidedWidget() ? 94 : 8;
        int baseY = isUsingAndOr() ? 60 : 30;

        List<WidgetRadioButton> radioButtons;
        WidgetRadioButton radioButton;
        if (isUsingAndOr()) {
            radioButtons = new ArrayList<>();
            radioButton = new WidgetRadioButton(guiLeft + baseX, guiTop + 30, 0xFF404040, xlate("pneumaticcraft.gui.progWidget.condition.anyBlock"),
                    b -> progWidget.setAndFunction(false));
            radioButton.checked = !progWidget.isAndFunction();
            addButton(radioButton);
            radioButtons.add(radioButton);
            radioButton.otherChoices = radioButtons;

            radioButton = new WidgetRadioButton(guiLeft + baseX, guiTop + 42, 0xFF404040, xlate("pneumaticcraft.gui.progWidget.condition.allBlocks"),
                    b -> progWidget.setAndFunction(true));
            radioButton.checked = progWidget.isAndFunction();
            addButton(radioButton);
            radioButtons.add(radioButton);
            radioButton.otherChoices = radioButtons;
        }

        if (requiresNumber()) {
            radioButtons = new ArrayList<>();
            for (ICondition.Operator op : ICondition.Operator.values()) {
                radioButton = new WidgetRadioButton(guiLeft + baseX, guiTop + baseY + op.ordinal() * 12, 0xFF404040,
                        new StringTextComponent(op.toString()), b -> progWidget.setOperator(op));
                radioButton.checked = progWidget.getOperator() == op;
                addButton(radioButton);
                radioButtons.add(radioButton);
                radioButton.otherChoices = radioButtons;
            }

            textField = new WidgetTextFieldNumber(font, guiLeft + baseX, guiTop + baseY + 40, 50, 11);
            textField.setText(progWidget.getRequiredCount() + "");
            textField.setFocused2(true);
            textField.setResponder(s -> progWidget.setRequiredCount(textField.getValue()));
            addButton(textField);
        }

        WidgetLabel label = addLabel(xlate("pneumaticcraft.gui.progWidget.condition.measure"), guiLeft + 8, guiTop + 152);
        label.setTooltip(xlate("pneumaticcraft.gui.progWidget.condition.measure.tooltip"));
        WidgetTextField measureTextField = new WidgetTextField(font, guiLeft + label.getWidth() + 8, guiTop + 150, 80, 11);
        measureTextField.setText(progWidget.getMeasureVar());
        measureTextField.setResponder(progWidget::setMeasureVar);
        addButton(measureTextField);
    }

    protected boolean isSidedWidget() {
        return progWidget instanceof ISidedWidget;
    }

    protected boolean isUsingAndOr() {
        return false;
    }

    protected boolean requiresNumber() {
        return true;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (isSidedWidget()) {
            font.func_243248_b(matrixStack, xlate("pneumaticcraft.gui.progWidget.inventory.accessingSides"), guiLeft + 4, guiTop + 20, 0xFF404060);
        }
        ITextComponent s = progWidget.getExtraStringInfo().get(0);
        font.func_243248_b(matrixStack, s, guiLeft + xSize / 2f - font.getStringPropertyWidth(s) / 2f, guiTop + 120, 0xFF404060);
    }

    public static class Item extends GuiProgWidgetDroneCondition<ProgWidgetDroneConditionItem> {
        public Item(ProgWidgetDroneConditionItem widget, GuiProgrammer guiProgrammer) {
            super(widget, guiProgrammer);
        }
    }

    public static class Fluid extends GuiProgWidgetDroneCondition<ProgWidgetDroneConditionFluid> {
        public Fluid(ProgWidgetDroneConditionFluid progWidget, GuiProgrammer guiProgrammer) {
            super(progWidget, guiProgrammer);
        }
    }

    public static class Pressure extends GuiProgWidgetDroneCondition<ProgWidgetDroneConditionPressure> {
        public Pressure(ProgWidgetDroneConditionPressure progWidget, GuiProgrammer guiProgrammer) {
            super(progWidget, guiProgrammer);
        }
    }

    public static class Energy extends GuiProgWidgetDroneCondition<ProgWidgetDroneConditionEnergy> {
        public Energy(ProgWidgetDroneConditionEnergy progWidget, GuiProgrammer guiProgrammer) {
            super(progWidget, guiProgrammer);
        }
    }
}
