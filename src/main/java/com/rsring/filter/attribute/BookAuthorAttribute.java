package com.rsring.filter.attribute;

import com.rsring.filter.ItemAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * 书作者属性 - 按成书作者过滤
 */
public class BookAuthorAttribute implements ItemAttribute {
    
    private String author;
    
    public BookAuthorAttribute() {
        this.author = "";
    }
    
    public BookAuthorAttribute(String author) {
        this.author = author != null ? author : "";
    }
    
    @Override
    public boolean appliesTo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String bookAuthor = extractAuthor(stack);
        return bookAuthor.equals(author);
    }
    
    @Override
    public List<ItemAttribute> listAttributesOf(ItemStack stack) {
        List<ItemAttribute> attributes = new ArrayList<>();
        if (stack.isEmpty()) return attributes;
        
        String bookAuthor = extractAuthor(stack);
        if (!bookAuthor.isEmpty()) {
            attributes.add(new BookAuthorAttribute(bookAuthor));
        }
        return attributes;
    }
    
    private String extractAuthor(ItemStack stack) {
        if (stack.getItem() instanceof ItemWrittenBook) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null && nbt.hasKey("author")) {
                return nbt.getString("author");
            }
        }
        return "";
    }
    
    @Override
    public String getTranslationKey() {
        return "book_author";
    }
    
    @Override
    public void writeNBT(NBTTagCompound nbt) {
        nbt.setString("author", author);
    }
    
    @Override
    public ItemAttribute readNBT(NBTTagCompound nbt) {
        return new BookAuthorAttribute(nbt.getString("author"));
    }
    
    @Override
    public Object[] getTranslationParameters() {
        return new Object[]{author};
    }
}
