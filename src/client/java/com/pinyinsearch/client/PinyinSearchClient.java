package com.pinyinsearch.client;

import com.pinyinsearch.client.search.PinyinDictionary;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口。
 *
 * <p>本模组没有服务端逻辑，所以 {@code fabric.mod.json} 里的 environment 为 client，
 * 且只注册 client entrypoint。</p>
 */
public final class PinyinSearchClient implements ClientModInitializer {
	public static final String MOD_ID = "pinyinsearch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// 提前加载字典，避免第一次打开创造物品栏时才在渲染线程做 I/O。
		int entries = PinyinDictionary.load();
		LOGGER.info("[PinyinSearch] 已加载 {} 个汉字拼音条目", entries);
	}
}
