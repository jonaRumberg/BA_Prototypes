module.exports = cds.service.impl(async function () {
  const northwind = await cds.connect.to("Northwind_Service");

  this.on("READ", "products", async (req) => {
    const query = req.req?.query?.q;

    if (query) {
      const results = await northwind.run(req.query);

      return results.filter((product) =>
        product.name.toLowerCase().includes(query.toLowerCase())
      );
    }

    return await northwind.run(req.query);
  });
});
