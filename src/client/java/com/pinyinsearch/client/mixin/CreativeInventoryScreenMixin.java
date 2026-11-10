package com.pinyinsearch.client.mixin;

import com.pinyinsearch.client.search.PinyinSearchHelper;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.search.SearchProvider;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * 1.20.1 创造物品栏拼音搜索 Mixin。
 *
 * <p>Yarn 1.20.1 中：
 * <ul>
 *     <li>{@code CreativeInventoryScreen#search()V} 是私有方法（intermediary: method_2464）；</li>
 *     <li>它内部调用 {@code SearchProvider#findAll(String)}（官方名 search，intermediary: method_4810）。</li>
 * </ul>
 *
 * <p>这里不覆盖整个 {@code search()}，只替换“从原版搜索提供器取结果”的这一行。
 * 这样标签搜索（#开头）、空搜索、滚动条重置等原版逻辑保持不动。</p>
 */
@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin {
	@Redirect(
			method = "search",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/search/SearchProvider;findAll(Ljava/lang/String;)Ljava/util/List;"
			)
	)
	private List<ItemStack> pinyinsearch$addPinyinMatches(SearchProvider<ItemStack> provider, String query) {
		return PinyinSearchHelper.search(provider, query);
	}
}
