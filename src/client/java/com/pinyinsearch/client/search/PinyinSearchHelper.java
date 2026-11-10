package com.pinyinsearch.client.search;

import com.pinyinsearch.client.PinyinSearchClient;
import net.minecraft.client.search.SearchProvider;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 创造物品栏搜索的扩展逻辑。
 *
 * <p>先保留原版 {@link SearchProvider#findAll(String)} 的结果，再把“中文名拼音/首字母”
 * 命中的物品追加进去。这样英文名、物品 ID、{#标签搜索} 等原版行为都不会被破坏。</p>
 */
public final class PinyinSearchHelper {
	private PinyinSearchHelper() {
	}

	/**
	 * @param provider 原版搜索提供器（Yarn 1.20.1 中方法名为 findAll）
	 * @param rawQuery 搜索框内容；原版调用前已经转成小写
	 */
	public static List<ItemStack> search(SearchProvider<ItemStack> provider, String rawQuery) {
		// 原版结果必须保留：英文名、命名空间 ID、标签搜索都依赖它。
		List<ItemStack> results = new ArrayList<>(provider.findAll(rawQuery == null ? "" : rawQuery));

		if (rawQuery == null || rawQuery.isEmpty() || rawQuery.startsWith("#")) {
			return results;
		}

		try {
			String lowerQuery = rawQuery.toLowerCase(Locale.ROOT).trim();
			// 去掉空格和分隔符，让“z s j”“xi'an”这类输入也能匹配。
			String compactQuery = compact(lowerQuery);
			String pinyinQuery = toPinyinQuery(lowerQuery);

			if (compactQuery.isEmpty()) {
				return results;
			}

			// 用 identity set 去重：原版搜索结果和 item group 里的 ItemStack 是同一批对象。
			Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			seen.addAll(results);

			List<ItemStack> candidates = collectCreativeStacks();

			if (pinyinQuery.isEmpty()) {
				// 纯中文查询：保持创造栏原有顺序。
				for (ItemStack stack : candidates) {
					if (!seen.contains(stack) && stack.getName().getString().toLowerCase(Locale.ROOT).contains(compactQuery)) {
						results.add(stack);
					}
				}
			} else {
				// 拼音查询：按匹配质量排序，再按名称长度、创造栏顺序排序。
				List<PinyinMatch> matches = new ArrayList<>();
				int order = 0;

				for (ItemStack stack : candidates) {
					if (seen.contains(stack)) {
						continue;
					}

					String displayName = stack.getName().getString();
					int score = PinyinProfile.of(displayName).score(pinyinQuery);

					if (score != PinyinProfile.NO_MATCH) {
						matches.add(new PinyinMatch(stack, score, displayName.length(), order));
					}

					order++;
				}

				matches.sort((left, right) -> {
					int comparison = Integer.compare(left.score, right.score);

					if (comparison != 0) {
						return comparison;
					}

					comparison = Integer.compare(left.nameLength, right.nameLength);

					if (comparison != 0) {
						return comparison;
					}

					return Integer.compare(left.order, right.order);
				});

				for (PinyinMatch match : matches) {
					results.add(match.stack);
				}
			}
		} catch (RuntimeException exception) {
			// 搜索框每输入一个字符都会走这里，任何异常都不应该让创造物品栏崩溃。
			PinyinSearchClient.LOGGER.error("[PinyinSearch] 拼音搜索失败，已回退到原版结果", exception);
		}

		return results;
	}

	/**
	 * 收集创造模式各标签页里参与搜索的物品。
	 *
	 * <p>1.20.1 的 {@link ItemGroup#getSearchTabStacks()} 正是原版建立搜索索引时使用的集合，
	 * 因此不会把生存模式不可获得、或当前标签页不显示的特殊物品额外加进来。</p>
	 */
	private static List<ItemStack> collectCreativeStacks() {
		List<ItemStack> stacks = new ArrayList<>();
		Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());

		for (ItemGroup group : ItemGroups.getGroups()) {
			// 跳过搜索标签页本身，避免读取上一次搜索留下的缓存。
			if (group.getType() == ItemGroup.Type.SEARCH) {
				continue;
			}

			for (ItemStack stack : group.getSearchTabStacks()) {
				if (!stack.isEmpty() && seen.add(stack)) {
					stacks.add(stack);
				}
			}
		}

		return stacks;
	}

	private static String compact(String text) {
		StringBuilder builder = new StringBuilder(text.length());

		for (int index = 0; index < text.length(); index++) {
			char character = text.charAt(index);

			if (!Character.isWhitespace(character) && character != '\'' && character != '"' && character != '-') {
				builder.append(character);
			}
		}

		return builder.toString();
	}

	/**
	 * 把查询规范化为无调拼音：只保留 a-z，并把 ü 统一成 v。
	 * 例如 "zhuàn shì" -> "zhuanshi"，"nü" -> "nv"。
	 */
	private static String toPinyinQuery(String text) {
		// ü 需要先转成 v，因为 NFD 会把 ü 拆成 u + 分音符，之后会丢失“鱼”的音。
		String prepared = text
				.replace('ü', 'v')
				.replace('ǖ', 'v')
				.replace('ǘ', 'v')
				.replace('ǚ', 'v')
				.replace('ǜ', 'v');
		String decomposed = Normalizer.normalize(prepared, Normalizer.Form.NFD);
		StringBuilder builder = new StringBuilder(decomposed.length());

		for (int index = 0; index < decomposed.length(); index++) {
			char character = decomposed.charAt(index);

			// 跳过声调符号等组合字符。
			if (Character.getType(character) == Character.NON_SPACING_MARK) {
				continue;
			}

			if (character >= 'a' && character <= 'z') {
				builder.append(character);
			}
		}

		return builder.toString();
	}

	private static final class PinyinMatch {
		private final ItemStack stack;
		private final int score;
		private final int nameLength;
		private final int order;

		private PinyinMatch(ItemStack stack, int score, int nameLength, int order) {
			this.stack = stack;
			this.score = score;
			this.nameLength = nameLength;
			this.order = order;
		}
	}
}
