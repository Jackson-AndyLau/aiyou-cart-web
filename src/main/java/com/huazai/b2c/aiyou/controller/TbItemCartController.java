package com.huazai.b2c.aiyou.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.huazai.b2c.aiyou.pojo.TbItem;
import com.huazai.b2c.aiyou.pojo.TbUser;
import com.huazai.b2c.aiyou.repo.AiyouResultData;
import com.huazai.b2c.aiyou.service.TbItemCartService;
import com.huazai.b2c.aiyou.service.TbItemService;
import com.huazai.b2c.aiyou.service.TbUserService;
import com.huazai.b2c.aiyou.utils.CookieUtils;
import com.huazai.b2c.aiyou.utils.JsonUtils;
import com.huazai.b2c.aiyou.vo.TbItemCartVO;

/**
 * 
 * @author HuaZai
 * @contact who.seek.me@java98k.vip
 *          <ul>
 * @description 商品购物车 Controller
 *              </ul>
 * @className TbItemCartController
 * @package com.huazai.b2c.aiyou.controller
 * @createdTime 2017年06月18日
 *
 * @version V1.0.0
 */
@Controller
@RequestMapping(value = "/item/cart")
public class TbItemCartController
{
	@Autowired
	private TbItemCartService tbItemCartService;

	@Autowired
	private TbUserService tbUserService;

	@Autowired
	private TbItemService tbItemService;

	@Value(value = "${TB_LOGIN_USER_INFO_KEY}")
	private String TB_LOGIN_USER_INFO_KEY;

	@Value(value = "${TB_ITEM_CART_LOCAL_KEY}")
	private String TB_ITEM_CART_LOCAL_KEY;

	@Value(value = "${TB_ITEM_CART_LOCAL_KEY_EXPIRE}")
	private Integer TB_ITEM_CART_LOCAL_KEY_EXPIRE;

	@Description(value = "商品添加购物车")
	@RequestMapping(value = "/add/{itemId}")
	public String addTbItemCart(@PathVariable(value = "itemId") Long itemId, Integer num, HttpServletRequest request,
			HttpServletResponse response)
	{
		// 获取用户Token
		String token = CookieUtils.getCookieValue(request, TB_LOGIN_USER_INFO_KEY);
		// 根据获取用户登录信息
		AiyouResultData resultData = tbUserService.getUserInfoByToken(token);
		if (resultData.getStatus() == 200)
		{
			// 用户已登录，则将商品添加到系统服务购物车
			TbItem tbItem = tbItemService.getTbItemById(itemId);
			TbItemCartVO tbItemCartVO = new TbItemCartVO();
			BeanUtils.copyProperties(tbItem, tbItemCartVO);

			TbUser tbUser = (TbUser) resultData.getData();
			tbItemCartService.addTbItemCart(tbUser.getId(), tbItemCartVO, num);
		} else
		{
			// 如果用户未登录，则将商品添加到本地Cookie
			this.updateTbItemCartToCookie(itemId, num, request, response);
		}
		return "cartSuccess";
	}

	/**
	 * 
	 * @author HuaZai
	 * @contact who.seek.me@java98k.vip
	 * @title addTbItemCartToCookie
	 *        <ul>
	 * @description 修改用户本地Cookie中商品信息
	 *              </ul>
	 * @createdTime 2017年06月18日
	 * @param itemId
	 * @param num
	 * @param request
	 * @param response
	 * @return void
	 *
	 * @version : V1.0.0
	 */
	private void updateTbItemCartToCookie(Long itemId, Integer num, HttpServletRequest request,
			HttpServletResponse response)
	{
		// 从Cookie中获取商品
		List<TbItemCartVO> tbItemCartVOs = this.getTbItemCartByCookie(request);
		if (!CollectionUtils.isEmpty(tbItemCartVOs))
		{
			// 如果商品存在于用户的Cookie中，则商品数量 + num
			boolean flag = false;
			for (TbItemCartVO tbItemCartVO : tbItemCartVOs)
			{
				if (tbItemCartVO.getId() == itemId.longValue())
				{
					tbItemCartVO.setNum(tbItemCartVO.getNum() + num);
					flag = true;
					break;
				}
			}
			// 将跟新后的购物车列表跟新到用户的本地Cookie中
			if (flag == true)
			{
				CookieUtils.setCookie(request, response, TB_ITEM_CART_LOCAL_KEY, JsonUtils.objectToJson(tbItemCartVOs),
						TB_ITEM_CART_LOCAL_KEY_EXPIRE, true);
			} else
			{
				// 如果商品不存在，根据商品ID查询商品信息
				TbItem tbItem = tbItemService.getTbItemById(itemId);
				TbItemCartVO tbItemCartVO = new TbItemCartVO();
				// 如果商品ID在商品库中存在
				if (!StringUtils.isEmpty(tbItem))
				{
					BeanUtils.copyProperties(tbItem, tbItemCartVO);
					// 设置商品数量
					tbItem.setNum(num);
					// 截取商品图片
					if (!StringUtils.isEmpty(tbItem.getImage()))
					{
						String img = tbItem.getImage().split(",")[0];
						tbItem.setImage(img);
					}
					// 将新增的商品设值到商品列表中
					tbItemCartVOs.add(tbItemCartVO);
					// 重新构建后的商品列表设值到用户本地的Cookie中
					CookieUtils.setCookie(request, response, TB_ITEM_CART_LOCAL_KEY,
							JsonUtils.objectToJson(tbItemCartVOs), TB_ITEM_CART_LOCAL_KEY_EXPIRE, true);
				}

			}
		}
	}

	/**
	 * 
	 * @author HuaZai
	 * @contact who.seek.me@java98k.vip
	 * @title deleteTbItemCartByCookie
	 *        <ul>
	 * @description 删除用户本地Cookie中购物车中的商品信息
	 *              </ul>
	 * @createdTime 2017年06月18日
	 * @param itemId
	 * @param request
	 * @param response
	 * @return void
	 *
	 * @version : V1.0.0
	 */
	private void deleteTbItemCartByCookie(Long itemId, HttpServletRequest request, HttpServletResponse response)
	{
		// 从Cookie中获取商品列表
		List<TbItemCartVO> tbItemCartVOs = this.getTbItemCartByCookie(request);
		boolean flag = false;
		if (!CollectionUtils.isEmpty(tbItemCartVOs))
		{
			for (TbItemCartVO tbItemCartVO : tbItemCartVOs)
			{
				// 如果删除的商品存在，则直接删除，否则，不做任何操作
				if (tbItemCartVO.getId() == itemId)
				{
					tbItemCartVOs.remove(tbItemCartVO);
					flag = true;
					break;
				}
			}
			if (flag == true)
			{
				// 更新购物车
				CookieUtils.setCookie(request, response, TB_ITEM_CART_LOCAL_KEY, JsonUtils.objectToJson(tbItemCartVOs),
						TB_ITEM_CART_LOCAL_KEY_EXPIRE, true);
			}
		}
	}

	/**
	 * 
	 * @author HuaZai
	 * @contact who.seek.me@java98k.vip
	 * @title getTbItemCartByCookie
	 *        <ul>
	 * @description 从用户本地的Cookie中获取商品列表信息
	 *              </ul>
	 * @createdTime 2017年06月18日
	 * @param request
	 * @return
	 * @return List<TbItemCartVO>
	 *
	 * @version : V1.0.0
	 */
	private List<TbItemCartVO> getTbItemCartByCookie(HttpServletRequest request)
	{
		// 从Cookie中获取商品信息
		String resultData = CookieUtils.getCookieValue(request, TB_ITEM_CART_LOCAL_KEY, true);
		// 将商品转换成列表并返回
		List<TbItemCartVO> tbItemCartVOs = new ArrayList<TbItemCartVO>();
		if (!StringUtils.isEmpty(resultData))
		{
			tbItemCartVOs = JsonUtils.jsonToList(resultData, TbItemCartVO.class);
		}
		return tbItemCartVOs;
	}

	@Description(value = "显示购物车列表")
	@RequestMapping(value = "/cart")
	public String showTbItemCart(HttpServletRequest request)
	{
		// 获取用户Token
		String token = CookieUtils.getCookieValue(request, TB_LOGIN_USER_INFO_KEY);
		// 根据获取用户登录信息
		AiyouResultData resultData = tbUserService.getUserInfoByToken(token);
		if (resultData.getStatus() == 200)
		{
			// 如果用户已登录，则展示服务器上面的购物车列表
			TbUser tbUser = (TbUser) resultData.getData();
			List<TbItemCartVO> tbItemCartVOs = tbItemCartService.queryTbItemCartByUserId(tbUser.getId());
			request.setAttribute("cartList", tbItemCartVOs);
		} else
		{
			// 如果用户未登录，则展示用户本地Cookie的购物车列表
			List<TbItemCartVO> tbItemCartVOs = this.getTbItemCartByCookie(request);
			request.setAttribute("cartList", tbItemCartVOs);
		}
		return "cart";
	}

	@Description(value = "修改购物车商品数量")
	@RequestMapping(value = "/update/num/{itemId}/{num}")
	public AiyouResultData updateTbItemCartByNum(@PathVariable(value = "itemId") Long itemId,
			@PathVariable(value = "num") Integer num, HttpServletRequest request, HttpServletResponse response)
	{
		// 获取用户Token
		String token = CookieUtils.getCookieValue(request, TB_LOGIN_USER_INFO_KEY);
		// 根据获取用户登录信息
		AiyouResultData resultData = tbUserService.getUserInfoByToken(token);
		if (resultData.getStatus() == 200)
		{
			// 如果登录，则修改服务器上的购物车商品数量
			TbUser tbUser = (TbUser) resultData.getData();
			tbItemCartService.updateTbItemCartByUserIdAndItemId(tbUser.getId(), itemId, num);
			return AiyouResultData.ok();
		} else
		{
			// 如果未登录，则修改用户本地Cookie中购物车商品数量
			this.updateTbItemCartToCookie(itemId, num, request, response);
			return AiyouResultData.ok();
		}
	}

	@Description(value = "删除购物车商品")
	@RequestMapping(value = "/delete/{itemId}")
	public String deleteTbItemCart(@PathVariable(value = "itemId") Long itemId, HttpServletRequest request,
			HttpServletResponse response)
	{
		// 获取用户Token
		String token = CookieUtils.getCookieValue(request, TB_LOGIN_USER_INFO_KEY);
		// 根据获取用户登录信息
		AiyouResultData resultData = tbUserService.getUserInfoByToken(token);
		if (resultData.getStatus() == 200)
		{
			// 如果登录，则删除服务器上的购物车商品数量
			TbUser tbUser = (TbUser) resultData.getData();
			tbItemCartService.deleteTbItemCartByUserIdAndItemId(tbUser.getId(), itemId);
		} else
		{
			// 如果未登录，则删除用户本地Cookie中购物车商品数量
			this.deleteTbItemCartByCookie(itemId, request, response);
		}
		return "redirect:/cart/cart.html";
	}

}
