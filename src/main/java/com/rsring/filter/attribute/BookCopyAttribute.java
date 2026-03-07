package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * 书副本属性 - 按成书副本等级过滤
 * 0=原版, 1=副本, 2=副本的副本, 3+=破损
 */
public class BookCopyAttribute implements ItemAttribute {
    
    private int generation;
    
    public BookCopyAttribute() {
        this.generation = -1;
    }
    
    public BookCopyAttribute(int generation) {
        this.generation = generation;
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        int bookGen = extractGeneration(stack);
        return bookGen == generation;
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) return attributes;
        
        int bookGen = extractGeneration(stack);
        if (bookGen >= 0) {
            attributes.add(new BookCopyAttribute(bookGen));
        }
        return attributes;
    }
    
    private int extractGeneration(ItemStack stack) {
        if (stack.getItem() instanceof ItemWrittenBook) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null && nbt.hasKey("generation")) {
                return nbt.getInteger("generation");
            }
        }
        return -1;
    }
    
    @Override
    public String getTranslationKey() {
        switch (generation) {
            case 0: return "book_copy_original";
            case 1: return "book_copy_first";
            case 2: return "book_copy_second";
            default: return "book_copy_tattered";
        }
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setInteger("generation", generation);
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        return new BookCopyAttribute(nbt.getInteger("generation"));
    }
    
    @Override
    public String getNBTKey() {
        return "book_copy";
    }
    
    @Override
    public boolean canRead(NBTTagCompound nbt) {
        return nbt.hasKey("book_copy");
    }
    
    @Override
    public Object[] getTranslationParameters() {
        return new Object[0];
    }
}
