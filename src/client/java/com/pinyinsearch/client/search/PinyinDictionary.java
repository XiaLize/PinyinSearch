package com.pinyinsearch.client.search;

import com.pinyinsearch.client.PinyinSearchClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 内置拼音字典。
 *
 * <p>资源文件：{@code assets/pinyinsearch/pinyin_dict.txt}。
 * 格式为每行 {@code 汉字<TAB>读音1,读音2}，例如：{@code 乐	le,yue}。
 * 字典只收录 1.20.1 原版中文物品名中出现的汉字，体积很小，不依赖 pinyin4j。</p>
 *
 * <p>多音字会保留多个读音，匹配时由 {@link PinyinProfile} 生成所有组合。</p>
 */
public final class PinyinDictionary {
	private static final String RESOURCE_PATH = "/assets/pinyinsearch/pinyin_dict.txt";
	private static volatile Map<Character, String[]> readings;

	private PinyinDictionary() {
	}

	/**
	 * 返回某个汉字的所有无调拼音；没有收录时返回 {@code null}。
	 */
	public static String[] get(char character) {
		return loadMap().get(character);
	}

	/**
	 * 加载字典并返回条目数。重复调用不会重复读取资源。
	 */
	public static int load() {
		return loadMap().size();
	}

	private static Map<Character, String[]> loadMap() {
		Map<Character, String[]> cached = readings;

		if (cached != null) {
			return cached;
		}

		synchronized (PinyinDictionary.class) {
			if (readings != null) {
				return readings;
			}

			Map<Character, String[]> map = new HashMap<>();

			try (InputStream stream = PinyinDictionary.class.getResourceAsStream(RESOURCE_PATH)) {
				if (stream == null) {
					PinyinSearchClient.LOGGER.error("[PinyinSearch] 找不到拼音字典资源：{}", RESOURCE_PATH);
				} else {
					readDictionary(stream, map);
				}
			} catch (IOException exception) {
				PinyinSearchClient.LOGGER.error("[PinyinSearch] 读取拼音字典失败，拼音搜索将不可用", exception);
			}

			readings = Collections.unmodifiableMap(map);
			return readings;
		}
	}

	private static void readDictionary(InputStream stream, Map<Character, String[]> target) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			int lineNumber = 0;

			while ((line = reader.readLine()) != null) {
				lineNumber++;

				// 兼容可能存在的 UTF-8 BOM。
				if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
					line = line.substring(1);
				}

				if (line.isEmpty() || line.charAt(0) == '#') {
					continue;
				}

				String[] parts = line.split("\t", 2);

				if (parts.length != 2 || parts[0].length() != 1) {
					PinyinSearchClient.LOGGER.warn("[PinyinSearch] 跳过格式错误的字典行 {}：{}", lineNumber, line);
					continue;
				}

				String[] pinyins = parts[1].split(",");

				if (pinyins.length > 0) {
					target.put(parts[0].charAt(0), pinyins);
				}
			}
		}
	}
}
