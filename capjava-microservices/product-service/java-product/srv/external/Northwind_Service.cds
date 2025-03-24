/* checksum : 12196625d5d6736054a467fca7ba5bfd */
@cds.external : true
@m.IsDefaultEntityContainer : 'true'
service Northwind_Service {};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Categories {
  key CategoryID : Integer not null;
  CategoryName : String(15) not null;
  Description : LargeString;
  Picture : LargeBinary;
  Products : Association to many Northwind_Service.Products {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.CustomerDemographics {
  key CustomerTypeID : String(10) not null;
  CustomerDesc : LargeString;
  Customers : Association to many Northwind_Service.Customers {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Customers {
  key CustomerID : String(5) not null;
  CompanyName : String(40) not null;
  ContactName : String(30);
  ContactTitle : String(30);
  Address : String(60);
  City : String(15);
  Region : String(15);
  PostalCode : String(10);
  Country : String(15);
  Phone : String(24);
  Fax : String(24);
  Orders : Association to many Northwind_Service.Orders {  };
  CustomerDemographics : Association to many Northwind_Service.CustomerDemographics {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Employees {
  key EmployeeID : Integer not null;
  LastName : String(20) not null;
  FirstName : String(10) not null;
  Title : String(30);
  TitleOfCourtesy : String(25);
  @odata.Type : 'Edm.DateTime'
  BirthDate : DateTime;
  @odata.Type : 'Edm.DateTime'
  HireDate : DateTime;
  Address : String(60);
  City : String(15);
  Region : String(15);
  PostalCode : String(10);
  Country : String(15);
  HomePhone : String(24);
  Extension : String(4);
  Photo : LargeBinary;
  Notes : LargeString;
  ReportsTo : Integer;
  PhotoPath : String(255);
  Employees1 : Association to many Northwind_Service.Employees {  };
  Employee1 : Association to Northwind_Service.Employees {  };
  Orders : Association to many Northwind_Service.Orders {  };
  Territories : Association to many Northwind_Service.Territories {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Order_Details {
  key OrderID : Integer not null;
  key ProductID : Integer not null;
  UnitPrice : Decimal(19, 4) not null;
  Quantity : Integer not null;
  @odata.Type : 'Edm.Single'
  Discount : Double not null;
  Order : Association to Northwind_Service.Orders {  };
  Product : Association to Northwind_Service.Products {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Orders {
  key OrderID : Integer not null;
  CustomerID : String(5);
  EmployeeID : Integer;
  @odata.Type : 'Edm.DateTime'
  OrderDate : DateTime;
  @odata.Type : 'Edm.DateTime'
  RequiredDate : DateTime;
  @odata.Type : 'Edm.DateTime'
  ShippedDate : DateTime;
  ShipVia : Integer;
  Freight : Decimal(19, 4);
  ShipName : String(40);
  ShipAddress : String(60);
  ShipCity : String(15);
  ShipRegion : String(15);
  ShipPostalCode : String(10);
  ShipCountry : String(15);
  Customer : Association to Northwind_Service.Customers {  };
  Employee : Association to Northwind_Service.Employees {  };
  Order_Details : Association to many Northwind_Service.Order_Details {  };
  Shipper : Association to Northwind_Service.Shippers {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Products {
  key ProductID : Integer not null;
  ProductName : String(40) not null;
  SupplierID : Integer;
  CategoryID : Integer;
  QuantityPerUnit : String(20);
  UnitPrice : Decimal(19, 4);
  UnitsInStock : Integer;
  UnitsOnOrder : Integer;
  ReorderLevel : Integer;
  Discontinued : Boolean not null;
  Category : Association to Northwind_Service.Categories {  };
  Order_Details : Association to many Northwind_Service.Order_Details {  };
  Supplier : Association to Northwind_Service.Suppliers {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Regions {
  key RegionID : Integer not null;
  RegionDescription : String(50) not null;
  Territories : Association to many Northwind_Service.Territories {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Shippers {
  key ShipperID : Integer not null;
  CompanyName : String(40) not null;
  Phone : String(24);
  Orders : Association to many Northwind_Service.Orders {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Suppliers {
  key SupplierID : Integer not null;
  CompanyName : String(40) not null;
  ContactName : String(30);
  ContactTitle : String(30);
  Address : String(60);
  City : String(15);
  Region : String(15);
  PostalCode : String(10);
  Country : String(15);
  Phone : String(24);
  Fax : String(24);
  HomePage : LargeString;
  Products : Association to many Northwind_Service.Products {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Territories {
  key TerritoryID : String(20) not null;
  TerritoryDescription : String(50) not null;
  RegionID : Integer not null;
  Region : Association to Northwind_Service.Regions {  };
  Employees : Association to many Northwind_Service.Employees {  };
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Alphabetical_list_of_products {
  key ProductID : Integer not null;
  key ProductName : String(40) not null;
  key Discontinued : Boolean not null;
  key CategoryName : String(15) not null;
  SupplierID : Integer;
  CategoryID : Integer;
  QuantityPerUnit : String(20);
  UnitPrice : Decimal(19, 4);
  UnitsInStock : Integer;
  UnitsOnOrder : Integer;
  ReorderLevel : Integer;
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Category_Sales_for_1997 {
  key CategoryName : String(15) not null;
  CategorySales : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Current_Product_Lists {
  key ProductID : Integer not null;
  key ProductName : String(40) not null;
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Customer_and_Suppliers_by_Cities {
  key CompanyName : String(40) not null;
  key Relationship : String(9) not null;
  City : String(15);
  ContactName : String(30);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Invoices {
  key CustomerName : String(40) not null;
  key Salesperson : String(31) not null;
  key OrderID : Integer not null;
  key ShipperName : String(40) not null;
  key ProductID : Integer not null;
  key ProductName : String(40) not null;
  key UnitPrice : Decimal(19, 4) not null;
  key Quantity : Integer not null;
  @odata.Type : 'Edm.Single'
  key Discount : Double not null;
  ShipName : String(40);
  ShipAddress : String(60);
  ShipCity : String(15);
  ShipRegion : String(15);
  ShipPostalCode : String(10);
  ShipCountry : String(15);
  CustomerID : String(5);
  Address : String(60);
  City : String(15);
  Region : String(15);
  PostalCode : String(10);
  Country : String(15);
  @odata.Type : 'Edm.DateTime'
  OrderDate : DateTime;
  @odata.Type : 'Edm.DateTime'
  RequiredDate : DateTime;
  @odata.Type : 'Edm.DateTime'
  ShippedDate : DateTime;
  ExtendedPrice : Decimal(19, 4);
  Freight : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Order_Details_Extendeds {
  key OrderID : Integer not null;
  key ProductID : Integer not null;
  key ProductName : String(40) not null;
  key UnitPrice : Decimal(19, 4) not null;
  key Quantity : Integer not null;
  @odata.Type : 'Edm.Single'
  key Discount : Double not null;
  ExtendedPrice : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Order_Subtotals {
  key OrderID : Integer not null;
  Subtotal : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Orders_Qries {
  key OrderID : Integer not null;
  key CompanyName : String(40) not null;
  CustomerID : String(5);
  EmployeeID : Integer;
  @odata.Type : 'Edm.DateTime'
  OrderDate : DateTime;
  @odata.Type : 'Edm.DateTime'
  RequiredDate : DateTime;
  @odata.Type : 'Edm.DateTime'
  ShippedDate : DateTime;
  ShipVia : Integer;
  Freight : Decimal(19, 4);
  ShipName : String(40);
  ShipAddress : String(60);
  ShipCity : String(15);
  ShipRegion : String(15);
  ShipPostalCode : String(10);
  ShipCountry : String(15);
  Address : String(60);
  City : String(15);
  Region : String(15);
  PostalCode : String(10);
  Country : String(15);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Product_Sales_for_1997 {
  key CategoryName : String(15) not null;
  key ProductName : String(40) not null;
  ProductSales : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Products_Above_Average_Prices {
  key ProductName : String(40) not null;
  UnitPrice : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Products_by_Categories {
  key CategoryName : String(15) not null;
  key ProductName : String(40) not null;
  key Discontinued : Boolean not null;
  QuantityPerUnit : String(20);
  UnitsInStock : Integer;
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Sales_by_Categories {
  key CategoryID : Integer not null;
  key CategoryName : String(15) not null;
  key ProductName : String(40) not null;
  ProductSales : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Sales_Totals_by_Amounts {
  key OrderID : Integer not null;
  key CompanyName : String(40) not null;
  SaleAmount : Decimal(19, 4);
  @odata.Type : 'Edm.DateTime'
  ShippedDate : DateTime;
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Summary_of_Sales_by_Quarters {
  key OrderID : Integer not null;
  @odata.Type : 'Edm.DateTime'
  ShippedDate : DateTime;
  Subtotal : Decimal(19, 4);
};

@cds.external : true
@cds.persistence.skip : true
entity Northwind_Service.Summary_of_Sales_by_Years {
  key OrderID : Integer not null;
  @odata.Type : 'Edm.DateTime'
  ShippedDate : DateTime;
  Subtotal : Decimal(19, 4);
};

