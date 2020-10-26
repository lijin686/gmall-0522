package com.atguigu.gmall.search.service;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrValueVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.baomidou.mybatisplus.extension.api.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SearchResponseVo search(SearchParamVo paramVo) {

        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, this.buildDsl(paramVo));
            SearchResponse searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
            SearchResponseVo responseVo = parseResult(searchResponse);
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        SearchResponseVo responseVo = new SearchResponseVo();
        //总命中数
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits();
        responseVo.setTotal(totalHits);
        //获取当前页数据
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit ->{
            try {
                String json = hitsHit.getSourceAsString();
                Goods goods = MAPPER.readValue(json, Goods.class);
                //使用高亮标题覆盖原来的标题
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                Text[] fragments = highlightField.getFragments();
                goods.setTitle(fragments[0].string());
                return goods;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        //获取聚合结果集
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms brandAggId = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandAggId.getBuckets();
        //把桶的集合转化为品牌的集合
        if(!CollectionUtils.isEmpty(buckets)){
            List<BrandEntity> brandEntityList = buckets.stream().map(bucket ->{
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                //获取品牌的名称和Logo
                Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) subAggregationMap.get("brandNameAgg");
                List<? extends Terms.Bucket> nameBuckets = brandNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(nameBuckets)){
                    String brandName = nameBuckets.get(0).getKeyAsString();
                    brandEntity.setName(brandName);
                }
                //设置logo
                ParsedStringTerms logoAgg = (ParsedStringTerms) subAggregationMap.get("logoAgg");
                List<? extends Terms.Bucket> brandLogoAgg = logoAgg.getBuckets();
                if(!CollectionUtils.isEmpty(brandLogoAgg)){
                    String brandLogo = brandLogoAgg.get(0).getKeyAsString();
                    brandEntity.setLogo(brandLogo);
                }
                return brandEntity;

            }).collect(Collectors.toList());
            responseVo.setBrands(brandEntityList);
        }

        //解析分类聚合结果集
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            List<CategoryEntity> categoryEntityList = categoryIdAggBuckets.stream().map(bucket ->{
                CategoryEntity categoryEntity = new CategoryEntity();
                Long keyAsNumber = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                //设置分类Id
                categoryEntity.setId(keyAsNumber);
                ParsedStringTerms categoryNameAgg =(ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(nameAggBuckets)){
                    //设置种类的名称
                    String nameCategory = nameAggBuckets.get(0).getKeyAsString();
                    categoryEntity.setName(nameCategory);
                }
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntityList);
        }

        //获取规格参数聚合结果集，解析出规格参数
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrIdAggBuckets)){
            List<SearchResponseAttrValueVo> filterList = attrIdAggBuckets.stream().map(attrIdAggBucket ->{
                SearchResponseAttrValueVo searchResponseAttrValueVo = new SearchResponseAttrValueVo();
                long attrId = ((Terms.Bucket) attrIdAggBucket).getKeyAsNumber().longValue();
                searchResponseAttrValueVo.setAttrId(attrId);
                //获取子聚合
                Map<String, Aggregation> stringAggregationMap = ((Terms.Bucket) attrIdAggBucket).getAggregations().asMap();
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) stringAggregationMap.get("attrNameAgg");
                List<? extends Terms.Bucket> attrNameAggBuckets = attrNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(attrNameAggBuckets)){
                    //获取规格参数的名称
                    String attrName = attrNameAggBuckets.get(0).getKeyAsString();
                    searchResponseAttrValueVo.setAttrName(attrName);
                }
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) stringAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if(!CollectionUtils.isEmpty(attrValueAggBuckets)){
                    //获取规格参数的值
                    List<String> attrValueList = attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    searchResponseAttrValueVo.setAttrValues(attrValueList);
                }
                return searchResponseAttrValueVo;

            }).collect(Collectors.toList());
            responseVo.setFilters(filterList);
        }

        return responseVo;
    }

    private SearchSourceBuilder buildDsl(SearchParamVo paramVo){

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        String keyword = paramVo.getKeyword();
        //判断搜索关键字是否为空
        if(StringUtils.isBlank(keyword)){
            return sourceBuilder;
        }


        // 1. 构建搜索条件，就是在搜索框中输入的内容
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);

        // 1.1. 构建匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));

        // 1.2. 构建过滤条件
        // 1.2.1. 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if(!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }

        // 1.2.2. 分类过滤
        List<Long> cid3 = paramVo.getCid3();
        if(!CollectionUtils.isEmpty(cid3)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",cid3));
        }

        // 1.2.3. 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if(priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
            if(priceFrom != null){
                rangeQueryBuilder.gte(priceFrom);
            }
            if(priceTo != null){
                rangeQueryBuilder.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        // 1.2.4. 库存过滤
        Boolean store = paramVo.getStore();
        if(store != null){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("store",store));
        }

        // 1.2.5. 规格参数的嵌套过滤
        // 参数格式 ["4:6G-8G-12G","5:128G-256G-512G"]
        List<String> props = paramVo.getProps();
        if(!CollectionUtils.isEmpty(props)){
            props.forEach(prop ->{
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                //按照 : 冒号进行切片，把规格参数的id和规格参数的值切开
                String[] attrs = StringUtils.split(prop, ":");
                if(attrs != null && attrs.length == 2){
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrId",attrs[0]));
                    //然后在按照 - 进行切边，把规格参数的值切开
                    String[] attrValues = StringUtils.split(attrs[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                }

            });
        }
        //http://localhost:18086/search?keyword=%E6%89%8B%E6%9C%BA&brandId=1,2,3&cid3=225,250&priceFrom=1000&priceTo=5000&store=false&props=4:6G-8G-12G&props=5:256G-521G

        // 2. 构建排序条件
        Integer sort = paramVo.getSort();
        if(sort != null){
            // 排序：1-价格升序 2-价格降序 3-新品降序 4-销量降序
            switch (sort){
                case 1 : sourceBuilder.sort("price", SortOrder.ASC);break;
                case 2 : sourceBuilder.sort("price", SortOrder.DESC);break;
                case 3 : sourceBuilder.sort("createTime", SortOrder.DESC);break;
                case 4 : sourceBuilder.sort("sales", SortOrder.DESC);break;
                default:
                    sourceBuilder.sort("_sorce",SortOrder.DESC);
                    break;
            }
        }



        // 3. 构建分页条件
//        Integer pageNum = paramVo.getPageNum();
//        Integer pageSize = paramVo.getPageSize();
        if(paramVo.getPageNum() != null){
            sourceBuilder.from((paramVo.getPageNum()-1)*paramVo.getPageSize());
            sourceBuilder.size(paramVo.getPageSize());
        }

        //http://localhost:18086/search?keyword=%E6%89%8B%E6%9C%BA&brandId=1,2,3&cid3=225,250&priceFrom=1000&priceTo=5000&store=false&props=4:6G-8G-12G&props=5:256G-521G&sort=1&pageNum=1

        // 4. 构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red;'/>").postTags("</font>"));

        // 5. 构建聚合
        // 5.1. 品牌聚合

        sourceBuilder.aggregation(
                AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );

        // 5.2. 分类聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );

        // 5.3. 规格参数的嵌套聚合
        sourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg","searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))
        );

        // 6. 结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "subTitle", "price", "defaultImage"}, null);

        //System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
