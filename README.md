# LightCache

## 简单介绍
> 在工业界，缓存几乎是实现高性能服务最重要的部分，不论是 池化思想 衍生出的一系列线程池产品，抑或是 无锁编程，或者是我们计算机组成基础的 局部性原理，深究其背后，都离不开缓存化的思想。

我理想中的缓存框架要满足一下几点要求

1. 架构要十分轻量级，稳定性要十分良好
1. 支持一级缓存和二级缓存，提高缓存的重用性。
1. 支持自定义 key。
1. 对于缓存失效的场景，要避免业务层出现“休克”现象。
## 如何下载
### Step1 添加仓库
```xml
<repositories>
   <repository>
     <id>jitpack.io</id>
     <url>https://jitpack.io</url>
   </repository>
</repositories>
```
### Step2 添加依赖
```xml
<dependency>
  <groupId>com.github.codingmaple</groupId>
  <artifactId>LightCache</artifactId>
  <version>0.1.3</version>
</dependency>
```
​

> 如果下载失败，请检查 maven setting.xml, 修改 mirrors


---

## 如何使用
​

举个例子，现在有一张User表，User 对象结构如下
```java
@Data
public class User{
    private Long id;
    private String account;
    private String password;
}
```
如果我们想要获取用户信息，一般我们会定义一个 UserService 类
```java
public interface UserService{
    public User getUserById(Long id);
}
```
然后去实现UserService方法
```java
@Service
public class UserServiceImpl implements UserService{
    
   	@Autowired
    private UserRepository userRepository;
    
    public User getUserById(Long id){
     // 从数据库查找并返回
     return userRepository.findUserById(id);
    }
}
```
此时我们如果想要将用户信息缓存起来，以便不用每次都从数据库查找，那么我们可以这样改写 UserService 类
```java
@Service
public class UserServiceImpl implements UserService{
  
    private final UserRepository userRepository;
    private final AbstractCacheService<User> userCacheService;
    
    public UserServiceImpl(UserRepository userRepository, CacheFactory cacheFactory){
        this.userRepository = userRepository;
        this.userCacheService = cacheFactory.createNormalStore(
        	"user",
            User.class
        );
    }
    
    public User getUserById(Long id){
     // 从数据库查找并返回
     return userCacheService.loadDataFromLocalCache( id, () -> {
         return userCacheService.findUserById(id);
     );
    }
                                                    
}
```
这样第二次请求用户信息的时候，就会默认从缓存中加载，而无需再次请求数据库了。

---

上述例子只是一个很简单的应用，除此之外，它还支持自定义存储类型，高性能深拷贝，lua 脚本删除，多注册中心等等。更多的使用可以参考 API 使用。



