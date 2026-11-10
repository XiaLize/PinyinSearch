package com.pinyinsearch.client.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 一个物品中文名对应的拼音索引。
 *
 * <p>同时保存“全拼”和“首字母”两种形式。多音字会产生多个组合，
 * 因此“音乐唱片”既能被 {@code yinyue} 匹配，也能被 {@code yl} / {@code yy} 匹配。</p>
 */
final class PinyinProfile {
	/**
	 * 防止极端多音字名称产生过多组合。原版物品名正常情况下远低于此值。
	 */
	private static final int MAX_VARIANTS = 48;

	/**
	 * 没有命中时的分数。
	 */
	static final int NO_MATCH = Integer.MAX_VALUE;

	private final List<String> fullPinyin;
	private final List<String> initials;

	private PinyinProfile(List<String> fullPinyin, List<String> initials) {
		this.fullPinyin = fullPinyin;
		this.initials = initials;
	}

	static PinyinProfile of(String displayName) {
		List<String> full = new ArrayList<>();
		List<String> initials = new ArrayList<>();
		full.add("");
		initials.add("");

		for (int offset = 0; offset < displayName.length(); ) {
			int codePoint = displayName.codePointAt(offset);
			offset += Character.charCount(codePoint);

			// 只处理汉字；空格、标点、英文和数字交给原版搜索。
			if (Character.UnicodeScript.of(codePoint) != Character.UnicodeScript.HAN) {
				continue;
			}

			String[] readings = PinyinDictionary.get((char) codePoint);

			if (readings == null || readings.length == 0) {
				continue;
			}

			full = append(full, readings, false);
			initials = append(initials, readings, true);
		}

		return new PinyinProfile(full, initials);
	}

	/**
	 * 计算匹配质量，分数越小越相关。
	 *
	 * <p>全拼允许从中间匹配（例如 {@code shijian} 命中“钻石剑”）；
	 * 首字母只从名称开头匹配，避免 {@code zsj} 把“棕色蜡烛”之类的名字也全部带出来。</p>
	 *
	 * @param query 已转小写、只保留 a-z 的查询串
	 */
	int score(String query) {
		int best = NO_MATCH;

		for (String candidate : fullPinyin) {
			if (candidate.equals(query)) {
				return 0;
			}

			if (candidate.startsWith(query)) {
				best = Math.min(best, 1);
			} else if (candidate.contains(query)) {
				best = Math.min(best, 2);
			}
		}

		for (String candidate : initials) {
			if (candidate.equals(query)) {
				best = Math.min(best, 3);
			} else if (candidate.startsWith(query)) {
				best = Math.min(best, 4);
			}
		}

		return best;
	}

	private static List<String> append(List<String> prefixes, String[] readings, boolean firstLetterOnly) {
		Set<String> next = new LinkedHashSet<>();

		for (String prefix : prefixes) {
			for (String reading : readings) {
				if (reading.isEmpty()) {
					continue;
				}

				String suffix = firstLetterOnly ? reading.substring(0, 1) : reading;
				next.add(prefix + suffix);

				if (next.size() >= MAX_VARIANTS) {
					return new ArrayList<>(next);
				}
			}
		}

		return new ArrayList<>(next);
	}
}
