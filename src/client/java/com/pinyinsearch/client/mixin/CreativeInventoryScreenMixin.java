package com.pinyinsearch.client.mixin;

import com.pinyinsearch.client.search.PinyinSearchHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchProvider;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Minecraft 1.21 创造物品栏拼音搜索 Mixin。
 *
 * <p>Yarn 1.21 中：
 * <ul>
 *     <li>{@code CreativeInventoryScreen#search()V} 仍是私有方法（intermediary: method_2464）；</li>
 *     <li>普通搜索使用 {@code SearchManager#getItemTooltipReloadFuture()} 提供的
 *         {@link SearchProvider}，最终调用 {@code SearchProvider#findAll(String)}；</li>
 *     <li>{@code #} 开头的标签搜索使用 {@code getItemTagReloadFuture()}，并且会先去掉
 *         {@code #} 再调用 {@code findAll}。</li>
 * </ul>
 *
 * <p>这里只替换“从原版搜索提供器取结果”的这一行。标签搜索必须原样放行，
 * 否则拼音结果会混进 {@code #} 标签查询；普通搜索则在原版结果后追加拼音匹配。</p>
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
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.getNetworkHandler() != null) {
			SearchManager searchManager = client.getNetworkHandler().getSearchManager();

			// 标签搜索的 query 已经被去掉了 '#'，不能再用 startsWith("#") 判断。
			// 直接比较 provider 实例，只有普通物品提示搜索才追加拼音结果。
			if (provider == searchManager.getItemTagReloadFuture()) {
				return provider.findAll(query);
			}
		}

		return PinyinSearchHelper.search(provider, query);
	}
}
